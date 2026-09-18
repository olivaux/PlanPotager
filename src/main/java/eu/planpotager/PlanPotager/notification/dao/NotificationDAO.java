package eu.planpotager.PlanPotager.notification.dao;

import eu.planpotager.PlanPotager.notification.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationDAO extends JpaRepository<Notification, Long> {

    List<Notification> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    @Query("select case when count(n) > 0 then true else false end from Notification n "
            + "where n.userEmail = :userEmail and n.message = :message and n.isRead = false")
    boolean existsUnreadByUserEmailAndMessage(@Param("userEmail") String userEmail, @Param("message") String message);
}
