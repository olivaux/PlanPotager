package eu.planpotager.PlanPotager.garden.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.planpotager.PlanPotager.plant.domain.Plant;
import eu.planpotager.PlanPotager.registry.domain.Family;
import eu.planpotager.PlanPotager.registry.domain.Species;
import eu.planpotager.PlanPotager.registry.domain.Type;
import eu.planpotager.PlanPotager.registry.domain.Variety;
import eu.planpotager.PlanPotager.user.domain.User;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GardenPlantTest {

    private GardenPlant newGardenPlant() {
        User user = new User("jane.doe@example.com");
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        Family family = new Family("Solanacees", new Type("Legume"));
        Species species = new Species("Tomate", 0.3, 3, 5, 2, family);
        Variety variety = new Variety("Tomate Cerise", 0.2, 3, 5, 2, species);
        Plant plant = new Plant(variety, "Graines du Midi", "jane.doe@example.com");
        return garden.addPlant(plant, 10, 20);
    }

    @Test
    void constructor_shouldDefaultToAPlanterState() {
        GardenPlant gardenPlant = newGardenPlant();

        assertThat(gardenPlant.getState()).isEqualTo(PlantState.A_PLANTER);
        assertThat(gardenPlant.getDatePlanted()).isNull();
    }

    @Test
    void setState_shouldStampDatePlanted_whenTransitioningToPlantee() {
        GardenPlant gardenPlant = newGardenPlant();

        gardenPlant.setState(PlantState.PLANTEE);

        assertThat(gardenPlant.getDatePlanted()).isEqualTo(LocalDate.now());
    }

    @Test
    void setState_shouldAdvanceThroughTheWholeWorkflow_whenFollowingTheOrder() {
        GardenPlant gardenPlant = newGardenPlant();

        gardenPlant.setState(PlantState.PLANTEE);
        assertThat(gardenPlant.getState()).isEqualTo(PlantState.PLANTEE);

        gardenPlant.setState(PlantState.A_RECOLTER);
        assertThat(gardenPlant.getState()).isEqualTo(PlantState.A_RECOLTER);

        gardenPlant.setState(PlantState.RECOLTEE);
        assertThat(gardenPlant.getState()).isEqualTo(PlantState.RECOLTEE);
    }

    @Test
    void setState_shouldThrow_whenSkippingAState() {
        GardenPlant gardenPlant = newGardenPlant();

        assertThatThrownBy(() -> gardenPlant.setState(PlantState.A_RECOLTER))
                .isInstanceOf(IllegalStateException.class);
        assertThat(gardenPlant.getState()).isEqualTo(PlantState.A_PLANTER);
    }

    @Test
    void setState_shouldThrow_whenMovingBackwards() {
        GardenPlant gardenPlant = newGardenPlant();
        gardenPlant.setState(PlantState.PLANTEE);

        assertThatThrownBy(() -> gardenPlant.setState(PlantState.A_PLANTER))
                .isInstanceOf(IllegalStateException.class);
        assertThat(gardenPlant.getState()).isEqualTo(PlantState.PLANTEE);
    }

    @Test
    void setPosition_shouldUpdateCoordinates_whenStillToPlant() {
        GardenPlant gardenPlant = newGardenPlant();

        gardenPlant.setPosition(30, 40);

        assertThat(gardenPlant.getX()).isEqualTo(30);
        assertThat(gardenPlant.getY()).isEqualTo(40);
    }

    @Test
    void setPosition_shouldThrow_oncePlanted() {
        GardenPlant gardenPlant = newGardenPlant();
        gardenPlant.setState(PlantState.PLANTEE);

        assertThatThrownBy(() -> gardenPlant.setPosition(30, 40))
                .isInstanceOf(IllegalStateException.class);
        assertThat(gardenPlant.getX()).isEqualTo(10);
        assertThat(gardenPlant.getY()).isEqualTo(20);
    }

    @Test
    void constructor_shouldDefaultToASingleVegetable() {
        GardenPlant gardenPlant = newGardenPlant();

        assertThat(gardenPlant.getMatrixX()).isEqualTo(1);
        assertThat(gardenPlant.getMatrixY()).isEqualTo(1);
    }

    @Test
    void setMatrix_shouldUpdateBothDimensions_evenOncePlanted() {
        GardenPlant gardenPlant = newGardenPlant();
        gardenPlant.setState(PlantState.PLANTEE);

        gardenPlant.setMatrix(2, 4);

        assertThat(gardenPlant.getMatrixX()).isEqualTo(2);
        assertThat(gardenPlant.getMatrixY()).isEqualTo(4);
    }

    @Test
    void setMatrix_shouldThrow_whenADimensionIsOutOfRange() {
        GardenPlant gardenPlant = newGardenPlant();

        assertThatThrownBy(() -> gardenPlant.setMatrix(0, 4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gardenPlant.setMatrix(2, GardenPlant.MAX_MATRIX_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(gardenPlant.getMatrixX()).isEqualTo(1);
        assertThat(gardenPlant.getMatrixY()).isEqualTo(1);
    }
}
