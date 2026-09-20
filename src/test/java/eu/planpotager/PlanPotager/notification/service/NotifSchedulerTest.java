package eu.planpotager.PlanPotager.notification.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.planpotager.PlanPotager.notification.dto.NotifDTO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotifSchedulerTest {

    @Mock
    private NotifService notifService;

    @InjectMocks
    private NotifScheduler notifScheduler;

    @Test
    void checkAndNotify_shouldQueryPlantStates_toDetectNotificationsToSend() {
        List<NotifDTO> pending = List.of(
                new NotifDTO(null, "Recolte disponible", "RECOLTE", false, LocalDateTime.now()));
        when(notifService.checkPlantStates()).thenReturn(pending);

        notifScheduler.checkAndNotify();

        verify(notifService).checkPlantStates();
    }

    @Test
    void purgeExpiredNotifications_shouldDelegateToService() {
        notifScheduler.purgeExpiredNotifications();

        verify(notifService).purgeExpiredNotifications();
    }
}
