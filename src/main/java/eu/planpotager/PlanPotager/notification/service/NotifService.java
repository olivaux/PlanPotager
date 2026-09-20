package eu.planpotager.PlanPotager.notification.service;

import eu.planpotager.PlanPotager.garden.dao.GardenDAO;
import eu.planpotager.PlanPotager.garden.domain.PlantState;
import eu.planpotager.PlanPotager.garden.dto.PlantToCheckDTO;
import eu.planpotager.PlanPotager.notification.dao.NotificationDAO;
import eu.planpotager.PlanPotager.notification.domain.Notification;
import eu.planpotager.PlanPotager.notification.dto.NotifDTO;
import eu.planpotager.PlanPotager.notification.dto.NotifKeyDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotifService {

    private static final String TYPE_TO_PLANT = "A_PLANTER";
    private static final String TYPE_TO_HARVEST = "A_RECOLTER";
    private static final List<PlantState> STATES_TO_CHECK = List.of(PlantState.A_PLANTER, PlantState.PLANTEE);
    private static final int PAGE_SIZE = 500;
    private static final int MAX_NOTIF_PAGE_SIZE = 100;

    // Duree de vie d'une notification, lue ou non. C'est aussi le delai avant de renotifier le meme message
    // a un utilisateur (rappel tant que la plante est dans le meme etat).
    private static final int NOTIF_RETENTION_DAYS = 30;

    private final NotificationDAO notificationDAO;
    private final GardenDAO gardenDAO;

    public NotifService(NotificationDAO notificationDAO, GardenDAO gardenDAO) {
        this.notificationDAO = notificationDAO;
        this.gardenDAO = gardenDAO;
    }

    @Transactional(readOnly = true)
    public List<NotifDTO> getNotificationsByUser(String userEmail, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_NOTIF_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return notificationDAO.findByUserEmail(userEmail, pageable).stream()
                .map(this::toNotifDTO)
                .toList();
    }

    public void setNotifAsRead(String userEmail, Long notifId) {
        // Meme erreur qu'une notification absente : on ne revele pas l'existence de celles des autres utilisateurs.
        Notification notification = notificationDAO.findById(notifId)
                .filter(found -> found.getUserEmail().equals(userEmail))
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        notification.setRead(true);
    }

    public List<NotifDTO> checkPlantStates() {
        LocalDate today = LocalDate.now();
        Set<NotifKeyDTO> alreadyNotified = new HashSet<>(
                notificationDAO.findKeysCreatedSince(today.atStartOfDay().minusDays(NOTIF_RETENTION_DAYS)));

        List<Notification> toSave = new ArrayList<>();
        Long lastId = 0L;
        List<PlantToCheckDTO> page;
        do {
            page = gardenDAO.findPlantsToCheck(STATES_TO_CHECK, lastId, PageRequest.ofSize(PAGE_SIZE));
            for (PlantToCheckDTO plant : page) {
                NotifDTO notif = buildDueNotif(plant, today);
                // add() renvoie false si la cle existe deja : couvre aussi les doublons dans le meme passage.
                if (notif != null && alreadyNotified.add(new NotifKeyDTO(plant.userEmail(), notif.message()))) {
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

    public int purgeExpiredNotifications() {
        return notificationDAO.purgeCreatedBefore(LocalDateTime.now().minusDays(NOTIF_RETENTION_DAYS));
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
}
