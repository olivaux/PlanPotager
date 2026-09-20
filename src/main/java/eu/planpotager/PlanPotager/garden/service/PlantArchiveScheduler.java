package eu.planpotager.PlanPotager.garden.service;

import eu.planpotager.PlanPotager.garden.dao.PlantArchiveDAO;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlantArchiveScheduler {

    private static final int RETENTION_YEARS = 1;

    private final PlantArchiveDAO plantArchiveDAO;

    public PlantArchiveScheduler(PlantArchiveDAO plantArchiveDAO) {
        this.plantArchiveDAO = plantArchiveDAO;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredArchives() {
        LocalDateTime cutoff = LocalDateTime.now().minusYears(RETENTION_YEARS);
        plantArchiveDAO.deleteArchivedBefore(cutoff);
    }
}
