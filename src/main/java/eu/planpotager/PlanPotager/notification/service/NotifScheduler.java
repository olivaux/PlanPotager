package eu.planpotager.PlanPotager.notification.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotifScheduler {

    private final NotifService notifService;

    public NotifScheduler(NotifService notifService) {
        this.notifService = notifService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void checkAndNotify() {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
