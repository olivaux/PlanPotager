package eu.planpotager.PlanPotager.garden.dto;

import eu.planpotager.PlanPotager.garden.domain.PlantState;
import java.time.LocalDate;

// Projection minimale d'une plante pour la verification quotidienne des etats : les valeurs de plantation
// et de recolte sont deja resolues (variete, a defaut espece), voir Variety.getEffective*.
public record PlantToCheckDTO(Long id, String userEmail, String varietyName, PlantState state, LocalDate datePlanted,
        Integer plantationStart, Integer plantationEnd, Integer harvestDuration) {

}
