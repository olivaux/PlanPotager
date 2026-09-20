package eu.planpotager.PlanPotager.garden.dao;

import eu.planpotager.PlanPotager.garden.domain.Garden;
import eu.planpotager.PlanPotager.garden.domain.PlantState;
import eu.planpotager.PlanPotager.garden.dto.PlantToCheckDTO;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GardenDAO extends JpaRepository<Garden, Long> {

    List<Garden> findByUserEmail(String userEmail);

    // Pagination par curseur (id) : stable et sans OFFSET. Projection en valeurs scalaires, sans charger d'entites.
    @Query("select new eu.planpotager.PlanPotager.garden.dto.PlantToCheckDTO("
            + "gp.id, g.user.email, v.name, gp.state, gp.datePlanted, "
            + "coalesce(v.plantationStart, s.plantationStart), coalesce(v.plantationEnd, s.plantationEnd), "
            + "coalesce(v.harvestDuration, s.harvestDuration)) "
            + "from GardenPlant gp join gp.garden g join gp.plant p join p.variety v join v.species s "
            + "where gp.state in :states and gp.id > :afterId order by gp.id")
    List<PlantToCheckDTO> findPlantsToCheck(@Param("states") List<PlantState> states, @Param("afterId") Long afterId,
            Pageable pageable);
}
