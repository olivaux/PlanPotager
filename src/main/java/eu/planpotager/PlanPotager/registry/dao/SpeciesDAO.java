package eu.planpotager.PlanPotager.registry.dao;

import eu.planpotager.PlanPotager.registry.domain.Species;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpeciesDAO extends JpaRepository<Species, String> {
}
