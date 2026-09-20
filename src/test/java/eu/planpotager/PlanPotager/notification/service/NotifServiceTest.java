package eu.planpotager.PlanPotager.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.planpotager.PlanPotager.garden.dao.GardenDAO;
import eu.planpotager.PlanPotager.garden.domain.PlantState;
import eu.planpotager.PlanPotager.garden.dto.PlantToCheckDTO;
import eu.planpotager.PlanPotager.notification.dao.NotificationDAO;
import eu.planpotager.PlanPotager.notification.domain.Notification;
import eu.planpotager.PlanPotager.notification.dto.NotifDTO;
import eu.planpotager.PlanPotager.notification.dto.NotifKeyDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

    private static PlantToCheckDTO plantToCheck(long id, PlantState state, LocalDate datePlanted, int plantationStart,
            int plantationEnd, int harvestDurationWeeks) {
        return new PlantToCheckDTO(id, USER_EMAIL, "Tomate Cerise", state, datePlanted, plantationStart, plantationEnd,
                harvestDurationWeeks);
    }

    private void givenPlantsToCheck(PlantToCheckDTO... plants) {
        when(gardenDAO.findPlantsToCheck(anyList(), eq(0L), any(Pageable.class))).thenReturn(List.of(plants));
    }

    private List<Notification> savedNotifications() {
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.captor();
        verify(notificationDAO).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void checkPlantStates_shouldNotify_whenAPlanterAndCurrentMonthInPlantationWindow() {
        int currentMonth = LocalDate.now().getMonthValue();
        givenPlantsToCheck(plantToCheck(1L, PlantState.A_PLANTER, null, currentMonth, currentMonth, 8));

        List<NotifDTO> result = notifService.checkPlantStates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("Plante Tomate Cerise à Planter");
        assertThat(result.get(0).type()).isEqualTo("A_PLANTER");
        assertThat(savedNotifications()).extracting(Notification::getUserEmail).containsExactly(USER_EMAIL);
    }

    @Test
    void checkPlantStates_shouldNotNotify_whenAPlanterButOutsidePlantationWindow() {
        int farMonth = shiftMonth(LocalDate.now().getMonthValue(), 6);
        givenPlantsToCheck(plantToCheck(1L, PlantState.A_PLANTER, null, farMonth, farMonth, 8));

        List<NotifDTO> result = notifService.checkPlantStates();

        assertThat(result).isEmpty();
        verify(notificationDAO, never()).saveAll(anyList());
    }

    @Test
    void checkPlantStates_shouldNotify_whenPlanteeAndHarvestDurationElapsed() {
        givenPlantsToCheck(plantToCheck(1L, PlantState.PLANTEE, LocalDate.now(), 1, 12, 0));

        List<NotifDTO> result = notifService.checkPlantStates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("Plante Tomate Cerise à Récolter");
        assertThat(result.get(0).type()).isEqualTo("A_RECOLTER");
    }

    @Test
    void checkPlantStates_shouldNotNotify_whenPlanteeButHarvestDurationNotElapsed() {
        givenPlantsToCheck(plantToCheck(1L, PlantState.PLANTEE, LocalDate.now(), 1, 12, 5));

        List<NotifDTO> result = notifService.checkPlantStates();

        assertThat(result).isEmpty();
        verify(notificationDAO, never()).saveAll(anyList());
    }

    @Test
    void checkPlantStates_shouldNotNotify_whenPlanteeWithoutPlantingDate() {
        givenPlantsToCheck(plantToCheck(1L, PlantState.PLANTEE, null, 1, 12, 0));

        assertThat(notifService.checkPlantStates()).isEmpty();
    }

    @Test
    void checkPlantStates_shouldSkip_whenRecentNotificationAlreadyExists_readOrNot() {
        int currentMonth = LocalDate.now().getMonthValue();
        givenPlantsToCheck(plantToCheck(1L, PlantState.A_PLANTER, null, currentMonth, currentMonth, 8));
        when(notificationDAO.findKeysCreatedSince(any(LocalDateTime.class))).thenReturn(List.of(
                new NotifKeyDTO(USER_EMAIL, "Plante Tomate Cerise à Planter")));

        List<NotifDTO> result = notifService.checkPlantStates();

        assertThat(result).isEmpty();
        verify(notificationDAO, never()).saveAll(anyList());
    }

    @Test
    void checkPlantStates_shouldStillNotify_whenSameMessageWasSentToAnotherUser() {
        int currentMonth = LocalDate.now().getMonthValue();
        givenPlantsToCheck(plantToCheck(1L, PlantState.A_PLANTER, null, currentMonth, currentMonth, 8));
        when(notificationDAO.findKeysCreatedSince(any(LocalDateTime.class))).thenReturn(List.of(
                new NotifKeyDTO("autre@example.com", "Plante Tomate Cerise à Planter")));

        assertThat(notifService.checkPlantStates()).hasSize(1);
    }

    @Test
    void checkPlantStates_shouldLookBackOverTheRetentionPeriod_whenCheckingDuplicates() {
        givenPlantsToCheck();

        notifService.checkPlantStates();

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.captor();
        verify(notificationDAO).findKeysCreatedSince(since.capture());
        assertThat(since.getValue()).isBefore(LocalDateTime.now().minusDays(29));
        assertThat(since.getValue()).isAfter(LocalDateTime.now().minusDays(32));
    }

    @Test
    void checkPlantStates_shouldNotifyOnce_whenTwoPlantsOfSameVarietyAreDueInTheSameRun() {
        int currentMonth = LocalDate.now().getMonthValue();
        givenPlantsToCheck(
                plantToCheck(1L, PlantState.A_PLANTER, null, currentMonth, currentMonth, 8),
                plantToCheck(2L, PlantState.A_PLANTER, null, currentMonth, currentMonth, 8));

        List<NotifDTO> result = notifService.checkPlantStates();

        assertThat(result).hasSize(1);
        assertThat(savedNotifications()).hasSize(1);
    }

    @Test
    void checkPlantStates_shouldSaveAllNotificationsInASingleCall() {
        int currentMonth = LocalDate.now().getMonthValue();
        givenPlantsToCheck(
                plantToCheck(1L, PlantState.A_PLANTER, null, currentMonth, currentMonth, 8),
                plantToCheck(2L, PlantState.PLANTEE, LocalDate.now(), 1, 12, 0));

        notifService.checkPlantStates();

        assertThat(savedNotifications()).extracting(Notification::getType)
                .containsExactlyInAnyOrder("A_PLANTER", "A_RECOLTER");
    }

    @Test
    void checkPlantStates_shouldFetchNextPageAfterLastId_whileThePageIsFull() {
        int farMonth = shiftMonth(LocalDate.now().getMonthValue(), 6);
        when(gardenDAO.findPlantsToCheck(anyList(), anyLong(), any(Pageable.class))).thenAnswer(invocation -> {
            long afterId = invocation.getArgument(1);
            int pageSize = invocation.<Pageable>getArgument(2).getPageSize();
            if (afterId != 0L) {
                return List.of();
            }
            return LongStream.rangeClosed(1, pageSize)
                    .mapToObj(id -> plantToCheck(id, PlantState.A_PLANTER, null, farMonth, farMonth, 8))
                    .toList();
        });

        notifService.checkPlantStates();

        ArgumentCaptor<Pageable> firstPage = ArgumentCaptor.captor();
        verify(gardenDAO).findPlantsToCheck(anyList(), eq(0L), firstPage.capture());
        long lastIdOfFirstPage = firstPage.getValue().getPageSize();
        verify(gardenDAO).findPlantsToCheck(anyList(), eq(lastIdOfFirstPage), any(Pageable.class));
    }

    @Test
    void getNotificationsByUser_shouldReturnDTOsOfRequestedPage_newestFirst() {
        Notification notification = new Notification("Plante Tomate à Planter", "A_PLANTER", LocalDateTime.now(), USER_EMAIL);
        when(notificationDAO.findByUserEmail(eq(USER_EMAIL), any(Pageable.class))).thenReturn(List.of(notification));

        List<NotifDTO> result = notifService.getNotificationsByUser(USER_EMAIL, 2, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("Plante Tomate à Planter");
        assertThat(result.get(0).isRead()).isFalse();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(notificationDAO).findByUserEmail(eq(USER_EMAIL), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }

    @Test
    void getNotificationsByUser_shouldClampPageAndSize() {
        when(notificationDAO.findByUserEmail(eq(USER_EMAIL), any(Pageable.class))).thenReturn(List.of());

        notifService.getNotificationsByUser(USER_EMAIL, -3, 100000);
        notifService.getNotificationsByUser(USER_EMAIL, 0, 0);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(notificationDAO, times(2)).findByUserEmail(eq(USER_EMAIL), pageable.capture());
        assertThat(pageable.getAllValues()).extracting(Pageable::getPageNumber).containsExactly(0, 0);
        assertThat(pageable.getAllValues()).extracting(Pageable::getPageSize).containsExactly(100, 1);
    }

    @Test
    void purgeExpiredNotifications_shouldDeleteNotificationsOlderThanRetentionPeriod() {
        when(notificationDAO.purgeCreatedBefore(any(LocalDateTime.class))).thenReturn(3);

        int purged = notifService.purgeExpiredNotifications();

        assertThat(purged).isEqualTo(3);
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.captor();
        verify(notificationDAO).purgeCreatedBefore(cutoff.capture());
        assertThat(cutoff.getValue()).isBefore(LocalDateTime.now().minusDays(29));
        assertThat(cutoff.getValue()).isAfter(LocalDateTime.now().minusDays(31));
    }

    @Test
    void setNotifAsRead_shouldMarkNotificationAsRead() {
        Notification notification = new Notification("Plante Tomate à Planter", "A_PLANTER", LocalDateTime.now(), USER_EMAIL);
        when(notificationDAO.findById(1L)).thenReturn(Optional.of(notification));

        notifService.setNotifAsRead(1L);

        assertThat(notification.getRead()).isTrue();
    }

    @Test
    void setNotifAsRead_shouldThrow_whenNotificationNotFound() {
        when(notificationDAO.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notifService.setNotifAsRead(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
