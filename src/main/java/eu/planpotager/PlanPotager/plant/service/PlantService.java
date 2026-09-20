package eu.planpotager.PlanPotager.plant.service;

import eu.planpotager.PlanPotager.plant.dao.PlantDAO;
import eu.planpotager.PlanPotager.plant.domain.Plant;
import eu.planpotager.PlanPotager.plant.dto.PlantDTO;
import eu.planpotager.PlanPotager.registry.dao.VarietyDAO;
import eu.planpotager.PlanPotager.registry.domain.Variety;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlantService {

    private final PlantDAO plantDAO;
    private final VarietyDAO varietyDAO;

    public PlantService(PlantDAO plantDAO, VarietyDAO varietyDAO) {
        this.plantDAO = plantDAO;
        this.varietyDAO = varietyDAO;
    }

    public PlantDTO addPlant(String varietyName, String comment, String userEmail) {
        Variety variety = varietyDAO.findById(varietyName)
                .orElseThrow(() -> new IllegalArgumentException("Variety not found"));

        Plant plant = new Plant(variety, comment, userEmail);

        plantDAO.save(plant);
        return toPlantDTO(plant);
    }

    public void removePlant(Long plantId, String userEmail) {
        Plant plant = plantDAO.findById(plantId)
                .orElseThrow(() -> new IllegalArgumentException("Plant not found"));

        if (!plant.getUserEmail().equals(userEmail)) {
            throw new IllegalArgumentException("User does not own this plant");
        }

        plantDAO.delete(plant);
    }

    @Transactional(readOnly = true)
    public List<PlantDTO> getAvailablePlants(String userEmail) {
        return plantDAO.findByUserEmail(userEmail).stream()
                .map(this::toPlantDTO)
                .toList();
    }

    private PlantDTO toPlantDTO(Plant plant) {
        Variety variety = plant.getVariety();

        return new PlantDTO(plant.getId(), variety.getName(), plant.getComment(),
                variety.getSpecies().getName(), variety.getEffectiveRadius());
    }
}
