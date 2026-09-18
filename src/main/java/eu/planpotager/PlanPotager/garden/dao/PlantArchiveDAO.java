package eu.planpotager.PlanPotager.garden.dao;

import eu.planpotager.PlanPotager.garden.domain.PlantArchive;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantArchiveDAO extends JpaRepository<PlantArchive, Long> {

    List<PlantArchive> findByArchivedAtBefore(LocalDateTime cutoff);
}
