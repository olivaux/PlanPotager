package eu.planpotager.PlanPotager.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.planpotager.PlanPotager.garden.dao.GardenDAO;
import eu.planpotager.PlanPotager.garden.domain.Garden;
import eu.planpotager.PlanPotager.garden.domain.GardenPlant;
import eu.planpotager.PlanPotager.garden.domain.PlantState;
import eu.planpotager.PlanPotager.notification.dao.NotificationDAO;
import eu.planpotager.PlanPotager.notification.domain.Notification;
import eu.planpotager.PlanPotager.notification.dto.NotifDTO;
import eu.planpotager.PlanPotager.plant.domain.Plant;
import eu.planpotager.PlanPotager.registry.domain.Family;
import eu.planpotager.PlanPotager.registry.domain.Species;
import eu.planpotager.PlanPotager.registry.domain.Type;
import eu.planpotager.PlanPotager.registry.domain.Variety;
import eu.planpotager.PlanPotager.user.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotifServiceTest {

    private static final String USER_EMAIL = "jane.doe@example.com";

    @Mock
    private NotificationDAO notificationDAO;

    @Mock
    private GardenDAO gardenDAO;

    @InjectMocks
    private NotifService notifService;

    private static int shiftMonth(int month, int offset) {
        return ((month - 1 + offset) % 12) + 1;
    }

    private GardenPlant plantInGarden(int plantationStart, int plantationEnd, int harvestDurationWeeks) {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        Family family = new Family("Solanacees", new Type("Legume"));
        Species species = new Species("Tomate", 0.3, plantationStart, plantationEnd, harvestDurationWeeks, family);
        Variety variety = new Variety("Tomate Cerise", 0.2, null, null, null, species);
        Plant plant = new Plant(variety, "Graines du Midi", USER_EMAIL);
        return garden.addPlant(plant, 0, 0);
    }

    @Test
    void checkPlantStates_shouldNotify_whenAPlanterAndCurrentMonthInPlantationWindow() {
        int currentMonth = LocalDate.now().getMonthValue();
        GardenPlant gardenPlant = plantInGarden(currentMonth, currentMonth, 8);
        when(gardenDAO.findAllGardenPlants()).thenReturn(List.of(gardenPlant));
        when(notificationDAO.existsUnreadByUserEmailAndMessage(eq(USER_EMAIL), anyString())).thenReturn(false);

        List<NotifDTO> result = notifService.checkPlantStates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("Plante Tomate Cerise à Planter");
        assertThat(result.get(0).type()).isEqualTo("A_PLANTER");
        verify(notificationDAO).save(any(Notification.class));
    }

    @Test
    void checkPlantStates_shouldNotNotify_whenAPlanterButOutsidePlantationWindow() {
        int currentMonth = LocalDate.now().getMonthValue();
        int farMonth = shiftMonth(currentMonth, 6);
        GardenPlant gardenPlant = plantInGarden(farMonth, farMonth, 8);
        when(gardenDAO.findAllGardenPlants()).thenReturn(List.of(gardenPlant));

        List<NotifDTO> result = notifService.checkPlantStates();

        assertThat(result).isEmpty();
        verify(notificationDAO, never()).save(any(Notification.class));
    }

    @Test
    void checkPlantStates_shouldNotify_whenPlanteeAndHarvestDurationElapsed() {
        GardenPlant gardenPlant = plantInGarden(1, 12, 0);
        gardenPlant.setState(PlantState.PLANTEE);
        when(gardenDAO.findAllGardenPlants()).thenReturn(List.of(gardenPlant));
        when(notificationDAO.existsUnreadByUserEmailAndMessage(eq(USER_EMAIL), anyString())).thenReturn(false);

        List<NotifDTO> result = notifService.checkPlantStates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("Plante Tomate Cerise à Récolter");
        assertThat(result.get(0).type()).isEqualTo("A_RECOLTER");
    }

    @Test
    void checkPlantStates_shouldNotNotify_whenPlanteeButHarvestDurationNotElapsed() {
        GardenPlant gardenPlant = plantInGarden(1, 12, 5);
        gardenPlant.setState(PlantState.PLANTEE);
        when(gardenDAO.findAllGardenPlants()).thenReturn(List.of(gardenPlant));

        List<NotifDTO> result = notifService.checkPlantStates();

        assertThat(result).isEmpty();
        verify(notificationDAO, never()).save(any(Notification.class));
    }

    @Test
    void checkPlantStates_shouldSkip_whenUnreadNotificationAlreadyExistsForThatPlant() {
        int currentMonth = LocalDate.now().getMonthValue();
        GardenPlant gardenPlant = plantInGarden(currentMonth, currentMonth, 8);
        when(gardenDAO.findAllGardenPlants()).thenReturn(List.of(gardenPlant));
        when(notificationDAO.existsUnreadByUserEmailAndMessage(USER_EMAIL, "Plante Tomate Cerise à Planter"))
                .thenReturn(true);

        List<NotifDTO> result = notifService.checkPlantStates();

        assertThat(result).isEmpty();
        verify(notificationDAO, never()).save(any(Notification.class));
    }

    @Test
    void addNotifications_shouldPersistOneNotificationEntityPerDTO() {
        NotifDTO notif = new NotifDTO(null, "Plante Tomate Cerise à Planter", "A_PLANTER", false, LocalDateTime.now());

        notifService.addNotifications(List.of(notif), USER_EMAIL);

        verify(notificationDAO).save(any(Notification.class));
    }

    @Test
    void getNotificationsByUser_shouldReturnDTOsForEveryNotificationOfUser() {
        Notification notification = new Notification("Plante Tomate à Planter", "A_PLANTER", LocalDateTime.now(), USER_EMAIL);
        when(notificationDAO.findByUserEmailOrderByCreatedAtDesc(USER_EMAIL)).thenReturn(List.of(notification));

        List<NotifDTO> result = notifService.getNotificationsByUser(USER_EMAIL);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("Plante Tomate à Planter");
        assertThat(result.get(0).isRead()).isFalse();
    }

    @Test
    void setNotifAsRead_shouldMarkNotificationAsRead_andPersist() {
        Notification notification = new Notification("Plante Tomate à Planter", "A_PLANTER", LocalDateTime.now(), USER_EMAIL);
        when(notificationDAO.findById(1L)).thenReturn(Optional.of(notification));

        notifService.setNotifAsRead(1L);

        assertThat(notification.getRead()).isTrue();
        verify(notificationDAO).save(notification);
    }

    @Test
    void setNotifAsRead_shouldThrow_whenNotificationNotFound() {
        when(notificationDAO.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notifService.setNotifAsRead(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
