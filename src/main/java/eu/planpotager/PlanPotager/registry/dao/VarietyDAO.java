package eu.planpotager.PlanPotager.registry.dao;

import eu.planpotager.PlanPotager.registry.domain.Variety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VarietyDAO extends JpaRepository<Variety, String> {

}
