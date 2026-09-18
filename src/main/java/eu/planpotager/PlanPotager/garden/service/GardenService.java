package eu.planpotager.PlanPotager.garden.service;

import eu.planpotager.PlanPotager.garden.dao.AreaDAO;
import eu.planpotager.PlanPotager.garden.dao.GardenDAO;
import eu.planpotager.PlanPotager.garden.dao.PlantArchiveDAO;
import eu.planpotager.PlanPotager.garden.domain.Area;
import eu.planpotager.PlanPotager.garden.domain.Garden;
import eu.planpotager.PlanPotager.garden.domain.GardenPlant;
import eu.planpotager.PlanPotager.garden.domain.PlantArchive;
import eu.planpotager.PlanPotager.garden.domain.PlantState;
import eu.planpotager.PlanPotager.garden.dto.AreaDTO;
import eu.planpotager.PlanPotager.garden.dto.AssociationLinkDTO;
import eu.planpotager.PlanPotager.garden.dto.GardenDTO;
import eu.planpotager.PlanPotager.garden.dto.GardenPlantDTO;
import eu.planpotager.PlanPotager.plant.dao.PlantDAO;
import eu.planpotager.PlanPotager.plant.domain.Plant;
import eu.planpotager.PlanPotager.registry.dto.AssociationDTO;
import eu.planpotager.PlanPotager.registry.service.RegistryService;
import eu.planpotager.PlanPotager.user.dao.UserDAO;
import eu.planpotager.PlanPotager.user.domain.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GardenService {

    private static final int ASSOCIATION_RADIUS_CM = 100;

    private final GardenDAO gardenDAO;
    private final AreaDAO areaDAO;
    private final UserDAO userDAO;
    private final PlantDAO plantDAO;
    private final PlantArchiveDAO plantArchiveDAO;
    private final RegistryService registryService;

    public GardenService(GardenDAO gardenDAO, AreaDAO areaDAO, UserDAO userDAO, PlantDAO plantDAO,
            PlantArchiveDAO plantArchiveDAO, RegistryService registryService) {
        this.gardenDAO = gardenDAO;
        this.areaDAO = areaDAO;
        this.userDAO = userDAO;
        this.plantDAO = plantDAO;
        this.plantArchiveDAO = plantArchiveDAO;
        this.registryService = registryService;

    }

    public GardenDTO createGarden(String name, Double longitude, Double latitude, String userEmail) {
        User user = userDAO.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Garden garden = new Garden(name, longitude, latitude, user);
        gardenDAO.save(garden);
        return toGardenDTO(garden);
    }

    public List<GardenDTO> getGardensByUser(String userEmail) {
        List<Garden> gardens = gardenDAO.findByUserEmail(userEmail);
        List<GardenDTO> gardenDTOs = gardens.stream()
            .map(this::toGardenDTO)
            .toList();
        return gardenDTOs;
    }

    public GardenDTO getGardenById(String userEmail, Long gardenId) {
        Garden garden = gardenDAO.findById(gardenId)
            .orElseThrow(() -> new IllegalArgumentException("Garden not found"));

        checkUserAccess(userEmail, garden);

        List<AssociationLinkDTO> associationLinks = recomputeAssociationScore(garden);
        gardenDAO.save(garden);

        return toGardenDTO(garden, associationLinks);
    }

    public GardenDTO updateGarden(String userEmail, Long gardenId, String name, Double longitude, Double latitude) {
        Garden garden = gardenDAO.findById(gardenId)
            .orElseThrow(() -> new IllegalArgumentException("Garden not found"));

        checkUserAccess(userEmail, garden);

        garden.setName(name);
        garden.setLongitude(longitude);
        garden.setLatitude(latitude);
        gardenDAO.save(garden);

        return toGardenDTO(garden);
    }

    public void deleteGarden(Long gardenId, String userEmail) {
        Garden garden = gardenDAO.findById(gardenId)
            .orElseThrow(() -> new IllegalArgumentException("Garden not found"));
        
        checkUserAccess(userEmail, garden);
        
        gardenDAO.delete(garden);
    }

    public GardenPlantDTO addPlantToGarden(String userEmail, Long gardenId, Long plantId, int x, int y) {
        Garden garden = gardenDAO.findById(gardenId)
            .orElseThrow(() -> new IllegalArgumentException("Garden not found"));

        checkUserAccess(userEmail, garden);

        Plant plant = plantDAO.findById(plantId)
            .orElseThrow(() -> new IllegalArgumentException("Plant not found"));

        GardenPlant gardenPlant = garden.addPlant(plant, x, y);
        recomputeAssociationScore(garden);
        gardenDAO.save(garden);

        return toGardenPlantDTO(gardenPlant);
    }

    public GardenPlantDTO getPlantCurrentPosition(String userEmail, Long gardenId, Long gardenPlantId) {
        Garden garden = gardenDAO.findById(gardenId)
            .orElseThrow(() -> new IllegalArgumentException("Garden not found"));

        checkUserAccess(userEmail, garden);

        GardenPlant gardenPlant = garden.findGardenPlant(gardenPlantId);
        return toGardenPlantDTO(gardenPlant);
    }

    public GardenDTO changePlantPosition(String userEmail, Long gardenId, Long gardenPlantId, int newX, int newY) {
        Garden garden = gardenDAO.findById(gardenId)
            .orElseThrow(() -> new IllegalArgumentException("Garden not found"));

        checkUserAccess(userEmail, garden);

        garden.updatePlantPosition(gardenPlantId, newX, newY);
        List<AssociationLinkDTO> associationLinks = recomputeAssociationScore(garden);
        gardenDAO.save(garden);

        return toGardenDTO(garden, associationLinks);
    }

    public List<PlantState> getAvailableStates() {
        return List.of(PlantState.values());
    }

    public List<GardenPlantDTO> getGardenPlants(String userEmail, Long gardenId) {
        Garden garden = gardenDAO.findById(gardenId)
            .orElseThrow(() -> new IllegalArgumentException("Garden not found"));

        checkUserAccess(userEmail, garden);

        return garden.getGardenPlants().stream()
            .map(this::toGardenPlantDTO)
            .toList();
    }

    public List<AreaDTO> getGardenAreas(String userEmail, Long gardenId) {
        Garden garden = gardenDAO.findById(gardenId)
            .orElseThrow(() -> new IllegalArgumentException("Garden not found"));

        checkUserAccess(userEmail, garden);

        return areaDAO.findByGardenId(gardenId).stream()
            .map(this::toAreaDTO)
            .toList();
    }

    public GardenDTO setPlantState(String userEmail, Long gardenId, Long gardenPlantId, PlantState state) {
        Garden garden = gardenDAO.findById(gardenId)
            .orElseThrow(() -> new IllegalArgumentException("Garden not found"));

        checkUserAccess(userEmail, garden);

        garden.setPlantState(gardenPlantId, state);

        if (state != PlantState.RECOLTEE) {
            gardenDAO.save(garden);
            return toGardenDTO(garden);
        }

        GardenPlant harvestedPlant = garden.findGardenPlant(gardenPlantId);
        plantArchiveDAO.save(new PlantArchive(harvestedPlant));
        garden.removePlant(gardenPlantId);

        List<AssociationLinkDTO> associationLinks = recomputeAssociationScore(garden);
        gardenDAO.save(garden);
        return toGardenDTO(garden, associationLinks);
    }

    public GardenDTO removePlantFromGarden(String userEmail, Long gardenId, Long gardenPlantId) {
        Garden garden = gardenDAO.findById(gardenId)
            .orElseThrow(() -> new IllegalArgumentException("Garden not found"));

        checkUserAccess(userEmail, garden);

        garden.removePlant(gardenPlantId);
        List<AssociationLinkDTO> associationLinks = recomputeAssociationScore(garden);
        gardenDAO.save(garden);
        return toGardenDTO(garden, associationLinks);
    }

    public AreaDTO addArea(String userEmail, Long gardenId, AreaDTO points) {
        Garden garden = gardenDAO.findById(gardenId)
            .orElseThrow(() -> new IllegalArgumentException("Garden not found"));

        checkUserAccess(userEmail, garden);

        Area area = new Area(garden, points);
        areaDAO.save(area);

        return toAreaDTO(area);
    }

    public AreaDTO updateArea(String userEmail, Long gardenId, Long areaId, AreaDTO points) {
        Area existing = areaDAO.findById(areaId)
            .orElseThrow(() -> new IllegalArgumentException("Area not found"));

        checkUserAccess(userEmail, existing.getGarden());

        existing.setPoints(points);
        areaDAO.save(existing);

        return toAreaDTO(existing);
    }

    public void deleteArea(String userEmail, Long gardenId, Long areaId) {
        Area existing = areaDAO.findById(areaId)
            .orElseThrow(() -> new IllegalArgumentException("Area not found"));

        checkUserAccess(userEmail, existing.getGarden());

        areaDAO.delete(existing);
    }

    private AreaDTO toAreaDTO(Area area) {
        return new AreaDTO(area.getId(), area.getLeftUpX(), area.getLeftUpY(), area.getRightUpX(), area.getRightUpY(),
                area.getRightDownX(), area.getRightDownY(), area.getLeftDownX(), area.getLeftDownY());
    }

    private GardenPlantDTO toGardenPlantDTO(GardenPlant gardenPlant) {
        return new GardenPlantDTO(gardenPlant.getId(), gardenPlant.getX(), gardenPlant.getY(),
                gardenPlant.getState(), gardenPlant.getPlant().getId());
    }

    private GardenDTO toGardenDTO(Garden garden) {
        return toGardenDTO(garden, List.of());
    }

    private GardenDTO toGardenDTO(Garden garden, List<AssociationLinkDTO> associationLinks) {
        return new GardenDTO(garden.getId(), garden.getName(), garden.getLongitude(), garden.getLatitude(),
                garden.getScore(), associationLinks);
    }

    // Recalcule sur toutes les paires du potager (pas seulement celles touchant le dernier changement) pour eviter tout double comptage.
    private List<AssociationLinkDTO> recomputeAssociationScore(Garden garden) {
        List<GardenPlant> gardenPlants = garden.getGardenPlants();
        List<AssociationLinkDTO> links = new ArrayList<>();
        int goodCount = 0;
        int badCount = 0;

        for (int i = 0; i < gardenPlants.size(); i++) {
            for (int j = i + 1; j < gardenPlants.size(); j++) {
                GardenPlant a = gardenPlants.get(i);
                GardenPlant b = gardenPlants.get(j);
                if (!withinAssociationRadius(a, b)) {
                    continue;
                }

                String speciesA = a.getPlant().getVariety().getSpecies().getName();
                String speciesB = b.getPlant().getVariety().getSpecies().getName();
                Optional<AssociationDTO> association = registryService.getAssociation(speciesA, speciesB);
                if (association.isEmpty()) {
                    continue;
                }

                boolean positive = association.get().positive();
                links.add(new AssociationLinkDTO(a.getId(), b.getId(), positive));
                if (positive) {
                    goodCount++;
                } else {
                    badCount++;
                }
            }
        }

        int total = goodCount + badCount;
        garden.setScore(total == 0 ? null : (double) goodCount / total * 10);

        return links;
    }

    private boolean withinAssociationRadius(GardenPlant a, GardenPlant b) {
        long dx = a.getX() - b.getX();
        long dy = a.getY() - b.getY();
        return dx * dx + dy * dy <= (long) ASSOCIATION_RADIUS_CM * ASSOCIATION_RADIUS_CM;
    }

    private void checkUserAccess(String userEmail, Garden garden) {
        if (!garden.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("User does not have access to this garden");
        }
    }

    
}
