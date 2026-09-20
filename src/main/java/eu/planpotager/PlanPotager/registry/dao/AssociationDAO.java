package eu.planpotager.PlanPotager.registry.dao;

import eu.planpotager.PlanPotager.registry.domain.Association;
import eu.planpotager.PlanPotager.registry.domain.AssociationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssociationDAO extends JpaRepository<Association, AssociationId> {

}
