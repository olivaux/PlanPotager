package eu.planpotager.PlanPotager.notification.service;

import eu.planpotager.PlanPotager.garden.dao.GardenDAO;
import eu.planpotager.PlanPotager.garden.domain.GardenPlant;
import eu.planpotager.PlanPotager.garden.domain.PlantState;
import eu.planpotager.PlanPotager.notification.dao.NotificationDAO;
import eu.planpotager.PlanPotager.notification.domain.Notification;
import eu.planpotager.PlanPotager.notification.dto.NotifDTO;
import eu.planpotager.PlanPotager.registry.domain.Variety;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotifService {

    private static final String TYPE_TO_PLANT = "A_PLANTER";
    private static final String TYPE_TO_HARVEST = "A_RECOLTER";

    private final NotificationDAO notificationDAO;
    private final GardenDAO gardenDAO;

    public NotifService(NotificationDAO notificationDAO, GardenDAO gardenDAO) {
        this.notificationDAO = notificationDAO;
        this.gardenDAO = gardenDAO;
    }

    @Transactional(readOnly = true)
    public List<NotifDTO> getNotificationsByUser(String userEmail) {
        return notificationDAO.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(this::toNotifDTO)
                .toList();
    }

    public void setNotifAsRead(Long notifId) {
        Notification notification = notificationDAO.findById(notifId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        notification.setRead(true);
    }

    public List<NotifDTO> checkPlantStates() {
        LocalDate today = LocalDate.now();
        Map<String, List<NotifDTO>> notifsByUser = new HashMap<>();

        for (GardenPlant gardenPlant : gardenDAO.findAllGardenPlants()) {
            NotifDTO notif = buildDueNotif(gardenPlant, today);
            if (notif == null) {
                continue;
            }

            String userEmail = gardenPlant.getGarden().getUser().getEmail();
            if (notificationDAO.existsUnreadByUserEmailAndMessage(userEmail, notif.message())) {
                continue;
            }

            notifsByUser.computeIfAbsent(userEmail, key -> new ArrayList<>()).add(notif);
        }

        notifsByUser.forEach((userEmail, notifs) -> addNotifications(notifs, userEmail));

        return notifsByUser.values().stream().flatMap(List::stream).toList();
    }

    public void addNotifications(List<NotifDTO> notifs, String userEmail) {
        for (NotifDTO notif : notifs) {
            notificationDAO.save(new Notification(notif.message(), notif.type(), notif.createdAt(), userEmail));
        }
    }

    private NotifDTO buildDueNotif(GardenPlant gardenPlant, LocalDate today) {
        Variety variety = gardenPlant.getPlant().getVariety();
        String plantName = variety.getName();

        if (gardenPlant.getState() == PlantState.A_PLANTER && isPlantingMonth(variety, today)) {
            return new NotifDTO(null, "Plante " + plantName + " à Planter", TYPE_TO_PLANT, false, LocalDateTime.now());
        }

        if (gardenPlant.getState() == PlantState.PLANTEE && isReadyToHarvest(gardenPlant, variety, today)) {
            return new NotifDTO(null, "Plante " + plantName + " à Récolter", TYPE_TO_HARVEST, false, LocalDateTime.now());
        }

        return null;
    }

    private boolean isPlantingMonth(Variety variety, LocalDate today) {
        int month = today.getMonthValue();
        int start = variety.getEffectivePlantationStart();
        int end = variety.getEffectivePlantationEnd();

        return start <= end ? (month >= start && month <= end) : (month >= start || month <= end);
    }

    private boolean isReadyToHarvest(GardenPlant gardenPlant, Variety variety, LocalDate today) {
        LocalDate datePlanted = gardenPlant.getDatePlanted();
        if (datePlanted == null) {
            return false;
        }

        LocalDate dateToHarvest = datePlanted.plusWeeks(variety.getEffectiveHarvestDuration());
        return !today.isBefore(dateToHarvest);
    }

    private NotifDTO toNotifDTO(Notification notification) {
        return new NotifDTO(notification.getId(), notification.getMessage(), notification.getType(),
                notification.getRead(), notification.getCreatedAt());
    }
}
