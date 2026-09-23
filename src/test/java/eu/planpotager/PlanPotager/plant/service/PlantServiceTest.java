package eu.planpotager.PlanPotager.plant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.planpotager.PlanPotager.plant.dao.PlantDAO;
import eu.planpotager.PlanPotager.plant.domain.Plant;
import eu.planpotager.PlanPotager.plant.dto.PlantDTO;
import eu.planpotager.PlanPotager.registry.dao.VarietyDAO;
import eu.planpotager.PlanPotager.registry.domain.Family;
import eu.planpotager.PlanPotager.registry.domain.Species;
import eu.planpotager.PlanPotager.registry.domain.Type;
import eu.planpotager.PlanPotager.registry.domain.Variety;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlantServiceTest {

    private static final String USER_EMAIL = "jane.doe@example.com";

    @Mock
    private PlantDAO plantDAO;

    @Mock
    private VarietyDAO varietyDAO;

    @InjectMocks
    private PlantService plantService;

    private Species tomato() {
        Family family = new Family("Solanacees");
        return new Species("Tomate", 0.3, 3, 5, 2, family, new Type("Legume"));
    }

    @Test
    void addPlant_shouldPersistPlant_andReturnMatchingDTO() {
        Variety variety = new Variety("Tomate Cerise", 0.2, 3, 5, 2, tomato());
        when(varietyDAO.findById("Tomate Cerise")).thenReturn(Optional.of(variety));
        when(plantDAO.save(any(Plant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlantDTO result = plantService.addPlant("Tomate Cerise", "Graines du Midi", USER_EMAIL);

        assertThat(result.variety()).isEqualTo("Tomate Cerise");
        assertThat(result.comment()).isEqualTo("Graines du Midi");
        assertThat(result.species()).isEqualTo("Tomate");
        assertThat(result.radius()).isEqualTo(0.2);
        verify(plantDAO).save(any(Plant.class));
    }

    @Test
    void addPlant_shouldThrow_whenVarietyDoesNotExist() {
        when(varietyDAO.findById("Variete Inconnue")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> plantService.addPlant("Variete Inconnue", "Graines du Midi", USER_EMAIL));
    }

    @Test
    void removePlant_shouldDeletePlant_whenItBelongsToUser() {
        Variety variety = new Variety("Tomate Cerise", 0.2, 3, 5, 2, tomato());
        Plant existing = new Plant(variety, "Graines du Midi", USER_EMAIL);
        when(plantDAO.findById(1L)).thenReturn(Optional.of(existing));

        plantService.removePlant(1L, USER_EMAIL);

        verify(plantDAO, times(1)).delete(existing);
    }

    @Test
    void getAvailablePlants_shouldReturnDTOsForEveryPlantOfUser() {
        Species tomato = tomato();
        Variety cherryTomato = new Variety("Tomate Cerise", 0.2, 3, 5, 2, tomato);
        Family carrotFamily = new Family("Apiacees");
        Species carrotSpecies = new Species("Carotte", 0.1, 3, 6, 3, carrotFamily, new Type("Legume"));
        Variety nantesCarrot = new Variety("Carotte de Nantes", 0.08, 3, 6, 3, carrotSpecies);

        Plant tomatoPlant = new Plant(cherryTomato, "Graines du Midi", USER_EMAIL);
        Plant carrotPlant = new Plant(nantesCarrot, "Vilmorin", USER_EMAIL);
        when(plantDAO.findByUserEmail(USER_EMAIL)).thenReturn(List.of(tomatoPlant, carrotPlant));

        List<PlantDTO> result = plantService.getAvailablePlants(USER_EMAIL);

        assertThat(result).extracting(PlantDTO::variety)
                .containsExactlyInAnyOrder("Tomate Cerise", "Carotte de Nantes");
        assertThat(result).extracting(PlantDTO::species)
                .containsExactlyInAnyOrder("Tomate", "Carotte");
    }

    @Test
    void getAvailablePlants_shouldFallBackToSpeciesRadius_whenVarietyRadiusIsNull() {
        Species tomato = tomato();
        Variety unknownRadiusVariety = new Variety("Tomate Ancienne", null, 3, 5, 2, tomato);
        Plant plant = new Plant(unknownRadiusVariety, "Graines du Midi", USER_EMAIL);
        when(plantDAO.findByUserEmail(USER_EMAIL)).thenReturn(List.of(plant));

        List<PlantDTO> result = plantService.getAvailablePlants(USER_EMAIL);

        assertThat(result).extracting(PlantDTO::radius).containsExactly(0.3);
    }

    @Test
    void getAvailablePlants_shouldReturnEmptyList_whenUserHasNoPlant() {
        when(plantDAO.findByUserEmail(USER_EMAIL)).thenReturn(List.of());

        List<PlantDTO> result = plantService.getAvailablePlants(USER_EMAIL);

        assertThat(result).isEmpty();
    }
}
