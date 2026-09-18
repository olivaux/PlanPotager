package eu.planpotager.PlanPotager.notification.ui;

import eu.planpotager.PlanPotager.notification.dto.NotifDTO;
import eu.planpotager.PlanPotager.notification.service.NotifService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notif")
public class NotifController {

    private final NotifService notifService;

    public NotifController(NotifService notifService) {
        this.notifService = notifService;
    }

    @GetMapping
    public ResponseEntity<List<NotifDTO>> getNotifications(@AuthenticationPrincipal OidcUser principal) {
        return ResponseEntity.ok(notifService.getNotificationsByUser(principal.getEmail()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> readNotif(@PathVariable Long id) {
        try {
            notifService.setNotifAsRead(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
