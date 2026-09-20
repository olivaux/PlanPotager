package eu.planpotager.PlanPotager.garden.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.planpotager.PlanPotager.garden.dao.AreaDAO;
import eu.planpotager.PlanPotager.garden.dao.GardenDAO;
import eu.planpotager.PlanPotager.garden.dao.PlantArchiveDAO;
import eu.planpotager.PlanPotager.garden.domain.Area;
import eu.planpotager.PlanPotager.garden.domain.Garden;
import eu.planpotager.PlanPotager.garden.domain.GardenPlant;
import eu.planpotager.PlanPotager.garden.domain.PlantArchive;
import eu.planpotager.PlanPotager.garden.domain.PlantState;
import eu.planpotager.PlanPotager.garden.dto.AreaDTO;
import eu.planpotager.PlanPotager.garden.dto.GardenDTO;
import eu.planpotager.PlanPotager.garden.dto.GardenPlantDTO;
import eu.planpotager.PlanPotager.plant.dao.PlantDAO;
import eu.planpotager.PlanPotager.plant.domain.Plant;
import eu.planpotager.PlanPotager.registry.domain.Family;
import eu.planpotager.PlanPotager.registry.domain.Species;
import eu.planpotager.PlanPotager.registry.domain.Type;
import eu.planpotager.PlanPotager.registry.domain.Variety;
import eu.planpotager.PlanPotager.registry.dto.AssociationDTO;
import eu.planpotager.PlanPotager.registry.service.RegistryService;
import eu.planpotager.PlanPotager.user.dao.UserDAO;
import eu.planpotager.PlanPotager.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GardenServiceTest {

    private static final String USER_EMAIL = "jane.doe@example.com";
    private static final String OTHER_USER_EMAIL = "intrus@example.com";

    @Mock
    private GardenDAO gardenDAO;

    @Mock
    private AreaDAO areaDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private PlantDAO plantDAO;

    @Mock
    private PlantArchiveDAO plantArchiveDAO;

    @Mock
    private RegistryService registryService;

    @InjectMocks
    private GardenService gardenService;

    private Variety cherryTomatoVariety() {
        Family family = new Family("Solanacees", new Type("Legume"));
        Species species = new Species("Tomate", 0.3, 3, 5, 2, family);
        return new Variety("Tomate Cerise", 0.2, 3, 5, 2, species);
    }

    private Plant plantOfSpecies(String speciesName) {
        Family family = new Family("Famille", new Type("Legume"));
        Species species = new Species(speciesName, 0.3, 3, 5, 2, family);
        Variety variety = new Variety(speciesName + " Variete", 0.2, 3, 5, 2, species);
        return new Plant(variety, "Fournisseur", USER_EMAIL);
    }

    @Test
    void createGarden_shouldPersistGarden_andReturnMatchingDTO() {
        User user = new User(USER_EMAIL);
        when(userDAO.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(gardenDAO.save(any(Garden.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GardenDTO result = gardenService.createGarden("Potager du fond", 2.35, 48.85, USER_EMAIL);

        assertThat(result.name()).isEqualTo("Potager du fond");
        assertThat(result.longitude()).isEqualTo(2.35);
        assertThat(result.latitude()).isEqualTo(48.85);
        verify(gardenDAO).save(any(Garden.class));
    }

    @Test
    void getGardensByUser_shouldReturnDTOsForEveryGardenOfUser() {
        Garden g1 = new Garden("Potager du fond", 2.35, 48.85, null);
        Garden g2 = new Garden("Potager avant", 2.36, 48.86, null);
        when(gardenDAO.findByUserEmail(USER_EMAIL)).thenReturn(List.of(g1, g2));

        List<GardenDTO> result = gardenService.getGardensByUser(USER_EMAIL);

        assertThat(result).extracting(GardenDTO::name)
                .containsExactlyInAnyOrder("Potager du fond", "Potager avant");
    }

    @Test
    void getGardensByUser_shouldReturnEmptyList_whenUserHasNoGarden() {
        when(gardenDAO.findByUserEmail(USER_EMAIL)).thenReturn(List.of());

        List<GardenDTO> result = gardenService.getGardensByUser(USER_EMAIL);

        assertThat(result).isEmpty();
    }

    @Test
    void getGardenById_shouldReturnMatchingDTO_whenGardenExists() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));

        GardenDTO result = gardenService.getGardenById(USER_EMAIL, 1L);

        assertThat(result.name()).isEqualTo("Potager du fond");
    }

    @Test
    void getGardenById_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, owner);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));

        assertThatThrownBy(() -> gardenService.getGardenById(OTHER_USER_EMAIL, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getGardenById_shouldComputeFullScore_withoutWritingIt_whenNearbyPlantsHaveGoodAssociation() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        garden.addPlant(plantOfSpecies("Tomate"), 0, 0);
        garden.addPlant(plantOfSpecies("Basilic"), 50, 0);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(registryService.getAssociation("Tomate", "Basilic"))
                .thenReturn(Optional.of(new AssociationDTO("Tomate", "Basilic", true)));

        GardenDTO result = gardenService.getGardenById(USER_EMAIL, 1L);

        assertThat(result.score()).isEqualTo(10.0);
        assertThat(result.associationLinks()).hasSize(1);
        assertThat(result.associationLinks().get(0).positive()).isTrue();
        assertThat(garden.getScore()).isNull();
        verify(gardenDAO, never()).save(any(Garden.class));
    }

    @Test
    void getGardenById_shouldComputeZeroScore_whenNearbyPlantsHaveBadAssociation() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        garden.addPlant(plantOfSpecies("Tomate"), 0, 0);
        garden.addPlant(plantOfSpecies("Fenouil"), 50, 0);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(registryService.getAssociation("Tomate", "Fenouil"))
                .thenReturn(Optional.of(new AssociationDTO("Tomate", "Fenouil", false)));

        GardenDTO result = gardenService.getGardenById(USER_EMAIL, 1L);

        assertThat(result.score()).isEqualTo(0.0);
        assertThat(result.associationLinks()).hasSize(1);
        assertThat(result.associationLinks().get(0).positive()).isFalse();
    }

    @Test
    void getGardenById_shouldAverageGoodAndBadAssociations_acrossMultiplePairs() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        garden.addPlant(plantOfSpecies("Tomate"), 0, 0);
        garden.addPlant(plantOfSpecies("Basilic"), 50, 0);
        garden.addPlant(plantOfSpecies("Fenouil"), 0, 50);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(registryService.getAssociation("Tomate", "Basilic"))
                .thenReturn(Optional.of(new AssociationDTO("Tomate", "Basilic", true)));
        when(registryService.getAssociation("Tomate", "Fenouil"))
                .thenReturn(Optional.of(new AssociationDTO("Tomate", "Fenouil", false)));

        GardenDTO result = gardenService.getGardenById(USER_EMAIL, 1L);

        assertThat(result.score()).isEqualTo(5.0);
        assertThat(result.associationLinks()).hasSize(2);
    }

    @Test
    void getGardenById_shouldReturnNullScore_whenNoAssociationRegisteredForNearbyPlants() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        garden.addPlant(plantOfSpecies("Tomate"), 0, 0);
        garden.addPlant(plantOfSpecies("Basilic"), 50, 0);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(registryService.getAssociation("Tomate", "Basilic")).thenReturn(Optional.empty());

        GardenDTO result = gardenService.getGardenById(USER_EMAIL, 1L);

        assertThat(result.score()).isNull();
        assertThat(result.associationLinks()).isEmpty();
    }

    @Test
    void getGardenById_shouldIgnorePlants_whenBeyondAssociationRadius() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        garden.addPlant(plantOfSpecies("Tomate"), 0, 0);
        garden.addPlant(plantOfSpecies("Basilic"), 200, 0);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));

        GardenDTO result = gardenService.getGardenById(USER_EMAIL, 1L);

        assertThat(result.score()).isNull();
        assertThat(result.associationLinks()).isEmpty();
        verifyNoInteractions(registryService);
    }

    @Test
    void updateGarden_shouldUpdateNameAndCoordinates() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Ancien nom", 0.0, 0.0, user);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));

        GardenDTO result = gardenService.updateGarden(USER_EMAIL, 1L, "Nouveau nom", 2.35, 48.85);

        assertThat(result.name()).isEqualTo("Nouveau nom");
        assertThat(result.longitude()).isEqualTo(2.35);
        assertThat(result.latitude()).isEqualTo(48.85);
        assertThat(garden.getName()).isEqualTo("Nouveau nom");
    }

    @Test
    void updateGarden_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = new Garden("Ancien nom", 0.0, 0.0, owner);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));

        assertThatThrownBy(() -> gardenService.updateGarden(OTHER_USER_EMAIL, 1L, "Nouveau nom", 2.35, 48.85))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteGarden_shouldDeleteGarden_whenItExists() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));

        gardenService.deleteGarden(1L, USER_EMAIL);

        verify(gardenDAO).delete(garden);
    }

    @Test
    void deleteGarden_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, owner);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));

        assertThatThrownBy(() -> gardenService.deleteGarden(1L, OTHER_USER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addPlantToGarden_shouldDelegateToGardenEntity_andFlushToGetPlantId() {
        User user = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        Plant plantRef = new Plant(cherryTomatoVariety(), "Graines du Midi", USER_EMAIL);
        GardenPlant createdGardenPlant = mock(GardenPlant.class);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(user);
        when(plantDAO.findById(42L)).thenReturn(Optional.of(plantRef));
        when(garden.addPlant(plantRef, 10, 20)).thenReturn(createdGardenPlant);
        when(createdGardenPlant.getId()).thenReturn(7L);
        when(createdGardenPlant.getX()).thenReturn(10);
        when(createdGardenPlant.getY()).thenReturn(20);
        when(createdGardenPlant.getState()).thenReturn(PlantState.A_PLANTER);
        when(createdGardenPlant.getPlant()).thenReturn(plantRef);

        GardenPlantDTO result = gardenService.addPlantToGarden(USER_EMAIL, 1L, 42L, 10, 20);

        verify(garden).addPlant(plantRef, 10, 20);
        verify(gardenDAO).flush();
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.x()).isEqualTo(10);
        assertThat(result.y()).isEqualTo(20);
    }

    @Test
    void addPlantToGarden_shouldRecomputeAssociationScore() {
        User user = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        Plant tomatoRef = plantOfSpecies("Tomate");
        Plant basilRef = plantOfSpecies("Basilic");
        GardenPlant existingBasilPlant = mock(GardenPlant.class);
        GardenPlant createdTomatoPlant = mock(GardenPlant.class);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(user);
        when(plantDAO.findById(42L)).thenReturn(Optional.of(tomatoRef));
        when(garden.addPlant(tomatoRef, 0, 0)).thenReturn(createdTomatoPlant);
        when(garden.getGardenPlants()).thenReturn(List.of(existingBasilPlant, createdTomatoPlant));
        when(existingBasilPlant.getX()).thenReturn(50);
        when(existingBasilPlant.getY()).thenReturn(0);
        when(existingBasilPlant.getPlant()).thenReturn(basilRef);
        when(createdTomatoPlant.getX()).thenReturn(0);
        when(createdTomatoPlant.getY()).thenReturn(0);
        when(createdTomatoPlant.getPlant()).thenReturn(tomatoRef);
        when(registryService.getAssociation("Basilic", "Tomate"))
                .thenReturn(Optional.of(new AssociationDTO("Basilic", "Tomate", true)));

        gardenService.addPlantToGarden(USER_EMAIL, 1L, 42L, 0, 0);

        verify(garden).setScore(10.0);
    }

    @Test
    void addPlantToGarden_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(owner);

        assertThatThrownBy(() -> gardenService.addPlantToGarden(OTHER_USER_EMAIL, 1L, 42L, 10, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getPlantCurrentPosition_shouldReturnDTO_forRequestedPlant() {
        User user = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        GardenPlant gardenPlant = mock(GardenPlant.class);
        Plant plant = new Plant(cherryTomatoVariety(), "Graines du Midi", USER_EMAIL);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(user);
        when(garden.findGardenPlant(42L)).thenReturn(gardenPlant);
        when(gardenPlant.getX()).thenReturn(10);
        when(gardenPlant.getY()).thenReturn(20);
        when(gardenPlant.getState()).thenReturn(PlantState.PLANTEE);
        when(gardenPlant.getPlant()).thenReturn(plant);

        GardenPlantDTO result = gardenService.getPlantCurrentPosition(USER_EMAIL, 1L, 42L);

        assertThat(result.x()).isEqualTo(10);
        assertThat(result.y()).isEqualTo(20);
        assertThat(result.state()).isEqualTo(PlantState.PLANTEE);
    }

    @Test
    void getPlantCurrentPosition_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(owner);

        assertThatThrownBy(() -> gardenService.getPlantCurrentPosition(OTHER_USER_EMAIL, 1L, 42L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changePlantPosition_shouldUpdatePositionOnEntity() {
        User user = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(user);
        when(garden.getId()).thenReturn(1L);

        GardenDTO result = gardenService.changePlantPosition(USER_EMAIL, 1L, 42L, 30, 40);

        verify(garden).updatePlantPosition(42L, 30, 40);
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void changePlantPosition_shouldRecomputeAssociationScore_afterMove() {
        User user = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        GardenPlant tomatoPlant = mock(GardenPlant.class);
        GardenPlant basilPlant = mock(GardenPlant.class);
        Plant tomatoRef = plantOfSpecies("Tomate");
        Plant basilRef = plantOfSpecies("Basilic");
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(user);
        when(garden.getGardenPlants()).thenReturn(List.of(tomatoPlant, basilPlant));
        when(tomatoPlant.getX()).thenReturn(0);
        when(tomatoPlant.getY()).thenReturn(0);
        when(tomatoPlant.getPlant()).thenReturn(tomatoRef);
        when(basilPlant.getX()).thenReturn(50);
        when(basilPlant.getY()).thenReturn(0);
        when(basilPlant.getPlant()).thenReturn(basilRef);
        when(registryService.getAssociation("Tomate", "Basilic"))
                .thenReturn(Optional.of(new AssociationDTO("Tomate", "Basilic", true)));

        gardenService.changePlantPosition(USER_EMAIL, 1L, 42L, 50, 0);

        verify(garden).updatePlantPosition(42L, 50, 0);
        verify(garden).setScore(10.0);
    }

    @Test
    void changePlantPosition_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(owner);

        assertThatThrownBy(() -> gardenService.changePlantPosition(OTHER_USER_EMAIL, 1L, 42L, 30, 40))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAvailableStates_shouldReturnAllFourStates() {
        List<PlantState> result = gardenService.getAvailableStates();

        assertThat(result).containsExactlyInAnyOrder(
                PlantState.A_PLANTER, PlantState.PLANTEE, PlantState.A_RECOLTER, PlantState.RECOLTEE);
    }

    @Test
    void setPlantState_shouldUpdateStateOnEntity() {
        User user = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(user);
        when(garden.getId()).thenReturn(1L);

        GardenDTO result = gardenService.setPlantState(USER_EMAIL, 1L, 42L, PlantState.PLANTEE);

        verify(garden).setPlantState(42L, PlantState.PLANTEE);
        verifyNoInteractions(plantArchiveDAO);
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void setPlantState_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(owner);

        assertThatThrownBy(() -> gardenService.setPlantState(OTHER_USER_EMAIL, 1L, 42L, PlantState.RECOLTEE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setPlantState_shouldArchiveAndRemovePlant_whenTransitioningToRecoltee() {
        User user = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        GardenPlant harvestedPlant = mock(GardenPlant.class);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(user);
        when(garden.findGardenPlant(42L)).thenReturn(harvestedPlant);
        when(harvestedPlant.getId()).thenReturn(42L);
        when(harvestedPlant.getPlant()).thenReturn(plantOfSpecies("Tomate"));
        when(harvestedPlant.getGarden()).thenReturn(garden);
        when(garden.getGardenPlants()).thenReturn(List.of());
        when(garden.getId()).thenReturn(1L);

        GardenDTO result = gardenService.setPlantState(USER_EMAIL, 1L, 42L, PlantState.RECOLTEE);

        verify(garden).setPlantState(42L, PlantState.RECOLTEE);
        verify(plantArchiveDAO).save(any(PlantArchive.class));
        verify(garden).removePlant(42L);
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void removePlantFromGarden_shouldRemovePlant() {
        User user = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(user);
        when(garden.getId()).thenReturn(1L);

        GardenDTO result = gardenService.removePlantFromGarden(USER_EMAIL, 1L, 42L);

        verify(garden).removePlant(42L);
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void removePlantFromGarden_shouldRecomputeAssociationScore_afterRemoval() {
        User user = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        GardenPlant remainingTomatoPlant = mock(GardenPlant.class);
        Plant tomatoRef = plantOfSpecies("Tomate");
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(user);
        when(garden.getGardenPlants()).thenReturn(List.of(remainingTomatoPlant));

        gardenService.removePlantFromGarden(USER_EMAIL, 1L, 42L);

        verify(garden).removePlant(42L);
        verify(garden).setScore(null);
        verifyNoInteractions(registryService);
    }

    @Test
    void removePlantFromGarden_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = mock(Garden.class);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(garden.getUser()).thenReturn(owner);

        assertThatThrownBy(() -> gardenService.removePlantFromGarden(OTHER_USER_EMAIL, 1L, 42L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addArea_shouldPersistArea_andReturnMatchingDTO() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(areaDAO.save(any(Area.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AreaDTO points = new AreaDTO(null, 0.0, 10.0, 10.0, 10.0, 10.0, 0.0, 0.0, 0.0);

        AreaDTO result = gardenService.addArea(USER_EMAIL, 1L, points);

        assertThat(result.leftUpX()).isEqualTo(0.0);
        assertThat(result.rightUpX()).isEqualTo(10.0);
        verify(areaDAO).save(any(Area.class));
    }

    @Test
    void addArea_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, owner);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        AreaDTO points = new AreaDTO(null, 0.0, 10.0, 10.0, 10.0, 10.0, 0.0, 0.0, 0.0);

        assertThatThrownBy(() -> gardenService.addArea(OTHER_USER_EMAIL, 1L, points))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateArea_shouldUpdatePointsOnExistingArea() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        Area existing = new Area(garden, 0.0, 0.0, 10.0, 0.0, 10.0, 10.0, 0.0, 10.0);
        when(areaDAO.findById(5L)).thenReturn(Optional.of(existing));
        AreaDTO newPoints = new AreaDTO(5L, 1.0, 1.0, 11.0, 1.0, 11.0, 11.0, 1.0, 11.0);

        AreaDTO result = gardenService.updateArea(USER_EMAIL, 1L, 5L, newPoints);

        assertThat(result.leftUpX()).isEqualTo(1.0);
    }

    @Test
    void updateArea_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, owner);
        Area existing = new Area(garden, 0.0, 0.0, 10.0, 0.0, 10.0, 10.0, 0.0, 10.0);
        when(areaDAO.findById(5L)).thenReturn(Optional.of(existing));
        AreaDTO newPoints = new AreaDTO(5L, 1.0, 1.0, 11.0, 1.0, 11.0, 11.0, 1.0, 11.0);

        assertThatThrownBy(() -> gardenService.updateArea(OTHER_USER_EMAIL, 1L, 5L, newPoints))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteArea_shouldDeleteArea_whenItExists() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        Area existing = new Area(garden, 0.0, 0.0, 10.0, 0.0, 10.0, 10.0, 0.0, 10.0);
        when(areaDAO.findById(5L)).thenReturn(Optional.of(existing));

        gardenService.deleteArea(USER_EMAIL, 1L, 5L);

        verify(areaDAO).delete(existing);
    }

    @Test
    void deleteArea_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, owner);
        Area existing = new Area(garden, 0.0, 0.0, 10.0, 0.0, 10.0, 10.0, 0.0, 10.0);
        when(areaDAO.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> gardenService.deleteArea(OTHER_USER_EMAIL, 1L, 5L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getGardenPlants_shouldReturnDTOsForEveryPlantInGarden() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        Plant plant = new Plant(cherryTomatoVariety(), "Graines du Midi", USER_EMAIL);
        garden.addPlant(plant, 10, 20);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));

        List<GardenPlantDTO> result = gardenService.getGardenPlants(USER_EMAIL, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).x()).isEqualTo(10);
        assertThat(result.get(0).y()).isEqualTo(20);
    }

    @Test
    void getGardenPlants_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, owner);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));

        assertThatThrownBy(() -> gardenService.getGardenPlants(OTHER_USER_EMAIL, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getGardenAreas_shouldReturnDTOsForEveryAreaInGarden() {
        User user = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, user);
        Area area = new Area(garden, 0.0, 0.0, 10.0, 0.0, 10.0, 10.0, 0.0, 10.0);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));
        when(areaDAO.findByGardenId(1L)).thenReturn(List.of(area));

        List<AreaDTO> result = gardenService.getGardenAreas(USER_EMAIL, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).leftUpX()).isEqualTo(0.0);
    }

    @Test
    void getGardenAreas_shouldThrow_whenUserDoesNotOwnGarden() {
        User owner = new User(USER_EMAIL);
        Garden garden = new Garden("Potager du fond", 2.35, 48.85, owner);
        when(gardenDAO.findById(1L)).thenReturn(Optional.of(garden));

        assertThatThrownBy(() -> gardenService.getGardenAreas(OTHER_USER_EMAIL, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
