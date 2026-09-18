package eu.planpotager.PlanPotager.garden.service;

import eu.planpotager.PlanPotager.garden.dao.PlantArchiveDAO;
import eu.planpotager.PlanPotager.garden.domain.PlantArchive;
import java.time.LocalDateTime;
import java.util.List;
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
        List<PlantArchive> expired = plantArchiveDAO.findByArchivedAtBefore(cutoff);
        plantArchiveDAO.deleteAll(expired);
    }
}
