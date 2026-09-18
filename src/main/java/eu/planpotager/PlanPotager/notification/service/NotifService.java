package eu.planpotager.PlanPotager.notification.service;

import eu.planpotager.PlanPotager.garden.dao.GardenDAO;
import eu.planpotager.PlanPotager.notification.dao.NotificationDAO;
import eu.planpotager.PlanPotager.notification.dto.NotifDTO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotifService {

    private final NotificationDAO notificationDAO;
    private final GardenDAO gardenDAO;

    public NotifService(NotificationDAO notificationDAO, GardenDAO gardenDAO) {
        this.notificationDAO = notificationDAO;
        this.gardenDAO = gardenDAO;
    }

    public List<NotifDTO> getNotificationsByUser(String userEmail) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public void setNotifAsRead(Long notifId) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public List<NotifDTO> checkPlantStates() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public void addNotifications(List<NotifDTO> notifs, String userEmail) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
