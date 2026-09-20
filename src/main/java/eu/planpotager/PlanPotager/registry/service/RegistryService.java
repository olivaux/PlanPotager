package eu.planpotager.PlanPotager.registry.service;

import eu.planpotager.PlanPotager.registry.dao.AssociationDAO;
import eu.planpotager.PlanPotager.registry.dao.SpeciesDAO;
import eu.planpotager.PlanPotager.registry.dao.VarietyDAO;
import eu.planpotager.PlanPotager.registry.domain.Association;
import eu.planpotager.PlanPotager.registry.domain.Species;
import eu.planpotager.PlanPotager.registry.domain.Variety;
import eu.planpotager.PlanPotager.registry.dto.AssociationDTO;
import eu.planpotager.PlanPotager.registry.dto.SpeciesDTO;
import eu.planpotager.PlanPotager.registry.dto.VarietyDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RegistryService {

    private final SpeciesDAO speciesDAO;
    private final VarietyDAO varietyDAO;
    private final AssociationDAO associationDAO;

    private volatile Map<SpeciesPair, AssociationDTO> associationsByPair;

    public RegistryService(SpeciesDAO speciesDAO, VarietyDAO varietyDAO, AssociationDAO associationDAO) {
        this.speciesDAO = speciesDAO;
        this.varietyDAO = varietyDAO;
        this.associationDAO = associationDAO;
    }

    public List<SpeciesDTO> getAllSpecies() {
        return speciesDAO.findAll().stream()
                .map(this::toSpeciesDTO)
                .toList();
    }

    private SpeciesDTO toSpeciesDTO(Species species) {
        return new SpeciesDTO(
                species.getName(),
                species.getRadius(),
                species.getPlantationStart(),
                species.getPlantationEnd(),
                species.getHarvestDuration());
    }

    public List<VarietyDTO> getVarietiesBySpecies(String speciesName) {
        return varietyDAO.findBySpeciesName(speciesName).stream()
                .map(this::toVarietyDTO)
                .toList();
    }

    private VarietyDTO toVarietyDTO(Variety variety) {
        return new VarietyDTO(
                variety.getName(),
                variety.getEffectiveRadius(),
                variety.getEffectivePlantationStart(),
                variety.getEffectivePlantationEnd(),
                variety.getEffectiveHarvestDuration());
    }

    public Optional<AssociationDTO> getAssociation(String speciesA, String speciesB) {
        return Optional.ofNullable(associationsByPair().get(SpeciesPair.of(speciesA, speciesB)));
    }

    // Le registre est en lecture seule : on le charge une seule fois, sans invalidation.
    private Map<SpeciesPair, AssociationDTO> associationsByPair() {
        Map<SpeciesPair, AssociationDTO> result = associationsByPair;
        if (result == null) {
            synchronized (this) {
                result = associationsByPair;
                if (result == null) {
                    result = loadAssociations();
                    associationsByPair = result;
                }
            }
        }
        return result;
    }

    private Map<SpeciesPair, AssociationDTO> loadAssociations() {
        Map<SpeciesPair, AssociationDTO> associations = new HashMap<>();
        for (Association association : associationDAO.findAll()) {
            String species = association.getSpecies().getName();
            String associatedSpecies = association.getAssociatedSpecies().getName();
            associations.putIfAbsent(SpeciesPair.of(species, associatedSpecies),
                    new AssociationDTO(species, associatedSpecies, association.isPositive()));
        }
        return Map.copyOf(associations);
    }

    // Cle non orientee : (A, B) et (B, A) designent la meme association.
    private record SpeciesPair(String first, String second) {

        static SpeciesPair of(String speciesA, String speciesB) {
            return speciesA.compareTo(speciesB) <= 0
                    ? new SpeciesPair(speciesA, speciesB)
                    : new SpeciesPair(speciesB, speciesA);
        }
    }
}
