package eu.planpotager.PlanPotager.notification.dao;

import eu.planpotager.PlanPotager.notification.domain.Notification;
import eu.planpotager.PlanPotager.notification.dto.NotifKeyDTO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationDAO extends JpaRepository<Notification, Long> {

    List<Notification> findByUserEmail(String userEmail, Pageable pageable);

    // Lues ou non : une notification lue ne doit pas etre recreee le lendemain.
    @Query("select new eu.planpotager.PlanPotager.notification.dto.NotifKeyDTO(n.userEmail, n.message) "
            + "from Notification n where n.createdAt >= :since")
    List<NotifKeyDTO> findKeysCreatedSince(@Param("since") LocalDateTime since);

    @Modifying
    @Query("delete from Notification n where n.createdAt < :cutoff")
    int purgeCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
