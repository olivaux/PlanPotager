package eu.planpotager.PlanPotager.notification.service;

import eu.planpotager.PlanPotager.garden.dao.GardenDAO;
import eu.planpotager.PlanPotager.garden.domain.PlantState;
import eu.planpotager.PlanPotager.garden.dto.PlantToCheckDTO;
import eu.planpotager.PlanPotager.notification.dao.NotificationDAO;
import eu.planpotager.PlanPotager.notification.domain.Notification;
import eu.planpotager.PlanPotager.notification.dto.NotifDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotifService {

    private static final String TYPE_TO_PLANT = "A_PLANTER";
    private static final String TYPE_TO_HARVEST = "A_RECOLTER";
    private static final List<PlantState> STATES_TO_CHECK = List.of(PlantState.A_PLANTER, PlantState.PLANTEE);
    private static final int PAGE_SIZE = 500;

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
        Set<NotifKey> unreadNotifs = new HashSet<>();
        for (Notification unread : notificationDAO.findUnread()) {
            unreadNotifs.add(new NotifKey(unread.getUserEmail(), unread.getMessage()));
        }

        List<Notification> toSave = new ArrayList<>();
        Long lastId = 0L;
        List<PlantToCheckDTO> page;
        do {
            page = gardenDAO.findPlantsToCheck(STATES_TO_CHECK, lastId, PageRequest.ofSize(PAGE_SIZE));
            for (PlantToCheckDTO plant : page) {
                NotifDTO notif = buildDueNotif(plant, today);
                if (notif != null && !unreadNotifs.contains(new NotifKey(plant.userEmail(), notif.message()))) {
                    toSave.add(new Notification(notif.message(), notif.type(), notif.createdAt(), plant.userEmail()));
                }
            }
            if (!page.isEmpty()) {
                lastId = page.get(page.size() - 1).id();
            }
        } while (page.size() == PAGE_SIZE);

        if (toSave.isEmpty()) {
            return List.of();
        }
        notificationDAO.saveAll(toSave);
        return toSave.stream()
                .map(this::toNotifDTO)
                .toList();
    }

    private NotifDTO buildDueNotif(PlantToCheckDTO plant, LocalDate today) {
        String plantName = plant.varietyName();

        if (plant.state() == PlantState.A_PLANTER && isPlantingMonth(plant, today)) {
            return new NotifDTO(null, "Plante " + plantName + " à Planter", TYPE_TO_PLANT, false, LocalDateTime.now());
        }

        if (plant.state() == PlantState.PLANTEE && isReadyToHarvest(plant, today)) {
            return new NotifDTO(null, "Plante " + plantName + " à Récolter", TYPE_TO_HARVEST, false, LocalDateTime.now());
        }

        return null;
    }

    private boolean isPlantingMonth(PlantToCheckDTO plant, LocalDate today) {
        int month = today.getMonthValue();
        int start = plant.plantationStart();
        int end = plant.plantationEnd();

        return start <= end ? (month >= start && month <= end) : (month >= start || month <= end);
    }

    private boolean isReadyToHarvest(PlantToCheckDTO plant, LocalDate today) {
        LocalDate datePlanted = plant.datePlanted();
        if (datePlanted == null) {
            return false;
        }

        LocalDate dateToHarvest = datePlanted.plusWeeks(plant.harvestDuration());
        return !today.isBefore(dateToHarvest);
    }

    private NotifDTO toNotifDTO(Notification notification) {
        return new NotifDTO(notification.getId(), notification.getMessage(), notification.getType(),
                notification.getRead(), notification.getCreatedAt());
    }

    // Un meme message ne doit pas etre renotifie tant que le precedent n'est pas lu, par utilisateur.
    private record NotifKey(String userEmail, String message) {
    }
}
