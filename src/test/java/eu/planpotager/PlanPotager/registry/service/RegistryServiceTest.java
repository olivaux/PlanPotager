package eu.planpotager.PlanPotager.registry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.planpotager.PlanPotager.registry.dao.AssociationDAO;
import eu.planpotager.PlanPotager.registry.dao.SpeciesDAO;
import eu.planpotager.PlanPotager.registry.dao.VarietyDAO;
import eu.planpotager.PlanPotager.registry.domain.Association;
import eu.planpotager.PlanPotager.registry.domain.Family;
import eu.planpotager.PlanPotager.registry.domain.Species;
import eu.planpotager.PlanPotager.registry.domain.Type;
import eu.planpotager.PlanPotager.registry.domain.Variety;
import eu.planpotager.PlanPotager.registry.dto.AssociationDTO;
import eu.planpotager.PlanPotager.registry.dto.SpeciesDTO;
import eu.planpotager.PlanPotager.registry.dto.VarietyDTO;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistryServiceTest {

    @Mock
    private SpeciesDAO speciesDAO;

    @Mock
    private VarietyDAO varietyDAO;

    @Mock
    private AssociationDAO associationDAO;

    @InjectMocks
    private RegistryService registryService;

    private Species species(String name) {
        Family family = new Family("Solanaceae", new Type("Légume"));
        return new Species(name, 0.3, 3, 5, 2, family);
    }

    @Test
    void getAllSpecies_shouldReturnDTOsForEverySpeciesInRegistry() {
        Family family = new Family("Solanaceae", new Type("Légume"));
        Species tomato = new Species("Tomate", 0.3, 3, 5, 2, family);
        Species pepper = new Species("Poivron", 0.25, 3, 5, 3, family);
        when(speciesDAO.findAll()).thenReturn(List.of(tomato, pepper));

        List<SpeciesDTO> result = registryService.getAllSpecies();

        assertThat(result).extracting(SpeciesDTO::name).containsExactlyInAnyOrder("Tomate", "Poivron");
    }

    @Test
    void getAllSpecies_shouldReturnEmptyList_whenRegistryIsEmpty() {
        when(speciesDAO.findAll()).thenReturn(List.of());

        List<SpeciesDTO> result = registryService.getAllSpecies();

        assertThat(result).isEmpty();
    }

    @Test
    void getVarietiesBySpecies_shouldReturnOnlyVarietiesOfRequestedSpecies() {
        Family family = new Family("Solanaceae", new Type("Légume"));
        Species tomato = new Species("Tomate", 0.3, 3, 5, 2, family);
        Variety cherry = new Variety("Cerise", 0.2, 3, 5, 2, tomato);
        Variety roma = new Variety("Roma", 0.3, 3, 5, 2, tomato);
        Variety basilVariety = new Variety("Grand vert", 0.2, 3, 5, 2, species("Basilic"));
        when(varietyDAO.findAll()).thenReturn(List.of(cherry, roma, basilVariety));

        List<VarietyDTO> result = registryService.getVarietiesBySpecies("Tomate");

        assertThat(result).extracting(VarietyDTO::name).containsExactlyInAnyOrder("Cerise", "Roma");
    }

    @Test
    void getVarietiesBySpecies_shouldReturnEmptyList_whenSpeciesHasNoVariety() {
        when(varietyDAO.findAll()).thenReturn(List.of());

        List<VarietyDTO> result = registryService.getVarietiesBySpecies("Inconnu");

        assertThat(result).isEmpty();
    }

    @Test
    void getVarietiesBySpecies_shouldLoadRegistryOnlyOnce_whateverTheRequestedSpecies() {
        Species tomato = species("Tomate");
        when(varietyDAO.findAll()).thenReturn(List.of(new Variety("Cerise", 0.2, 3, 5, 2, tomato)));

        registryService.getVarietiesBySpecies("Tomate");
        registryService.getVarietiesBySpecies("Tomate");
        registryService.getVarietiesBySpecies("Inconnu");

        verify(varietyDAO, times(1)).findAll();
    }

    @Test
    void getAllSpecies_shouldLoadRegistryOnlyOnce_acrossSuccessiveCalls() {
        when(speciesDAO.findAll()).thenReturn(List.of(species("Tomate")));

        registryService.getAllSpecies();
        registryService.getAllSpecies();

        verify(speciesDAO, times(1)).findAll();
    }

    @Test
    void getAssociation_shouldReturnAssociation_whenStoredInRequestedOrder() {
        Species tomato = species("Tomate");
        Species basil = species("Basilic");
        Association association = new Association(tomato, basil, true);
        when(associationDAO.findAll()).thenReturn(List.of(association));

        Optional<AssociationDTO> result = registryService.getAssociation("Tomate", "Basilic");

        assertThat(result).isPresent();
        assertThat(result.get().positive()).isTrue();
    }

    @Test
    void getAssociation_shouldReturnAssociation_whenStoredInReverseOrder() {
        Species tomato = species("Tomate");
        Species fennel = species("Fenouil");
        Association association = new Association(fennel, tomato, false);
        when(associationDAO.findAll()).thenReturn(List.of(association));

        Optional<AssociationDTO> result = registryService.getAssociation("Tomate", "Fenouil");

        assertThat(result).isPresent();
        assertThat(result.get().positive()).isFalse();
    }

    @Test
    void getAssociation_shouldReturnEmpty_whenNoAssociationRegistered() {
        when(associationDAO.findAll()).thenReturn(List.of());

        Optional<AssociationDTO> result = registryService.getAssociation("Tomate", "Basilic");

        assertThat(result).isEmpty();
    }

    @Test
    void getAssociation_shouldLoadRegistryOnlyOnce_acrossSuccessiveCalls() {
        Association association = new Association(species("Tomate"), species("Basilic"), true);
        when(associationDAO.findAll()).thenReturn(List.of(association));

        registryService.getAssociation("Tomate", "Basilic");
        registryService.getAssociation("Basilic", "Tomate");
        registryService.getAssociation("Tomate", "Fenouil");

        verify(associationDAO, times(1)).findAll();
    }
}
