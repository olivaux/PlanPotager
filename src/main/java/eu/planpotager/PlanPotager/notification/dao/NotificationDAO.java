package eu.planpotager.PlanPotager.notification.dao;

import eu.planpotager.PlanPotager.notification.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationDAO extends JpaRepository<Notification, Long> {

    List<Notification> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    @Query("select n from Notification n where n.isRead = false")
    List<Notification> findUnread();
}
