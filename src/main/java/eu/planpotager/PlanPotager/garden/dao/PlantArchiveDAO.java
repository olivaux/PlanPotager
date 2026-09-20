package eu.planpotager.PlanPotager.garden.dao;

import eu.planpotager.PlanPotager.garden.domain.PlantArchive;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PlantArchiveDAO extends JpaRepository<PlantArchive, Long> {

    @Transactional
    @Modifying
    @Query("delete from PlantArchive a where a.archivedAt < :cutoff")
    int deleteArchivedBefore(@Param("cutoff") LocalDateTime cutoff);
}
