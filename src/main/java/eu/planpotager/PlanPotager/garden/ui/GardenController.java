package eu.planpotager.PlanPotager.garden.ui;

import eu.planpotager.PlanPotager.garden.domain.GardenPlant;
import eu.planpotager.PlanPotager.garden.domain.PlantState;
import eu.planpotager.PlanPotager.garden.dto.AddPlantToGardenRequest;
import eu.planpotager.PlanPotager.garden.dto.AreaDTO;
import eu.planpotager.PlanPotager.garden.dto.GardenDTO;
import eu.planpotager.PlanPotager.garden.dto.GardenPlantDTO;
import eu.planpotager.PlanPotager.garden.dto.GardenRequest;
import eu.planpotager.PlanPotager.garden.dto.SetMatrixRequest;
import eu.planpotager.PlanPotager.garden.dto.SetStateRequest;
import eu.planpotager.PlanPotager.garden.service.GardenService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/garden")
public class GardenController {

    private final GardenService gardenService;

    public GardenController(GardenService gardenService) {
        this.gardenService = gardenService;
    }

    @PostMapping
    public ResponseEntity<GardenDTO> createGarden(@RequestBody GardenRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        GardenDTO garden = gardenService.createGarden(request.name(), request.longitude(), request.latitude(),
                principal.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(garden);
    }

    @GetMapping("")
    public ResponseEntity<List<GardenDTO>> getGardensByUser(@AuthenticationPrincipal OidcUser principal) {
        List<GardenDTO> gardens = gardenService.getGardensByUser(principal.getEmail());
        return ResponseEntity.ok(gardens);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GardenDTO> getGardenById(@PathVariable Long id,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            GardenDTO garden = gardenService.getGardenById(principal.getEmail(), id);
            return ResponseEntity.ok(garden);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<GardenDTO> updateGarden(@PathVariable Long id,
            @RequestBody GardenRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            GardenDTO garden = gardenService.updateGarden(principal.getEmail(), id,
                    request.name(), request.longitude(), request.latitude());
            return ResponseEntity.ok(garden);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGarden(@PathVariable Long id,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            gardenService.deleteGarden(id, principal.getEmail());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{gardenId}/plant")
    public ResponseEntity<GardenPlantDTO> addPlantToGarden(@PathVariable Long gardenId,
            @RequestBody AddPlantToGardenRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            GardenPlantDTO gardenPlant = gardenService.addPlantToGarden(principal.getEmail(), gardenId,
                    request.plantId(), request.x(), request.y());
            return ResponseEntity.status(HttpStatus.CREATED).body(gardenPlant);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{gardenId}/plant/{gardenPlantId}")
    public ResponseEntity<GardenPlantDTO> getPlantCurrentPosition(@PathVariable Long gardenId,
            @PathVariable Long gardenPlantId,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            GardenPlantDTO gardenPlant = gardenService.getPlantCurrentPosition(principal.getEmail(), gardenId, gardenPlantId);
            return ResponseEntity.ok(gardenPlant);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{gardenId}/plant/{gardenPlantId}/position")
    public ResponseEntity<GardenDTO> changePlantPosition(@PathVariable Long gardenId,
            @PathVariable Long gardenPlantId,
            @RequestBody Map<String, Integer> position,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            GardenDTO garden = gardenService.changePlantPosition(principal.getEmail(), gardenId, gardenPlantId,
                    position.get("x"), position.get("y"));
            return ResponseEntity.ok(garden);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{gardenId}/plant/{gardenPlantId}/matrix")
    public ResponseEntity<GardenPlantDTO> changePlantMatrix(@PathVariable Long gardenId,
            @PathVariable Long gardenPlantId,
            @RequestBody SetMatrixRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        if (!GardenPlant.isValidMatrixSize(request.x()) || !GardenPlant.isValidMatrixSize(request.y())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            GardenPlantDTO gardenPlant = gardenService.changePlantMatrix(principal.getEmail(), gardenId,
                    gardenPlantId, request.x(), request.y());
            return ResponseEntity.ok(gardenPlant);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{gardenId}/plants")
    public ResponseEntity<List<GardenPlantDTO>> getGardenPlants(@PathVariable Long gardenId,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            List<GardenPlantDTO> plants = gardenService.getGardenPlants(principal.getEmail(), gardenId);
            return ResponseEntity.ok(plants);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{gardenId}/areas")
    public ResponseEntity<List<AreaDTO>> getGardenAreas(@PathVariable Long gardenId,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            List<AreaDTO> areas = gardenService.getGardenAreas(principal.getEmail(), gardenId);
            return ResponseEntity.ok(areas);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{gardenId}/plant/{plantId}/states")
    public ResponseEntity<List<PlantState>> getAvailableStates(@PathVariable Long gardenId,
            @PathVariable Long plantId) {
        return ResponseEntity.ok(gardenService.getAvailableStates());
    }

    @PutMapping("/{gardenId}/plant/{gardenPlantId}/state")
    public ResponseEntity<GardenDTO> setPlantState(@PathVariable Long gardenId,
            @PathVariable Long gardenPlantId,
            @RequestBody SetStateRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            GardenDTO garden = gardenService.setPlantState(principal.getEmail(), gardenId, gardenPlantId, request.state());
            return ResponseEntity.ok(garden);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{gardenId}/plant/{gardenPlantId}")
    public ResponseEntity<GardenDTO> removePlantFromGarden(@PathVariable Long gardenId,
            @PathVariable Long gardenPlantId,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            GardenDTO garden = gardenService.removePlantFromGarden(principal.getEmail(), gardenId, gardenPlantId);
            return ResponseEntity.ok(garden);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{gardenId}/area")
    public ResponseEntity<AreaDTO> addArea(@PathVariable Long gardenId,
            @RequestBody AreaDTO points,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            AreaDTO area = gardenService.addArea(principal.getEmail(), gardenId, points);
            return ResponseEntity.status(HttpStatus.CREATED).body(area);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{gardenId}/area/{areaId}")
    public ResponseEntity<AreaDTO> updateArea(@PathVariable Long gardenId,
            @PathVariable Long areaId,
            @RequestBody AreaDTO points,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            AreaDTO area = gardenService.updateArea(principal.getEmail(), gardenId, areaId, points);
            return ResponseEntity.ok(area);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{gardenId}/area/{areaId}")
    public ResponseEntity<Void> deleteArea(@PathVariable Long gardenId,
            @PathVariable Long areaId,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            gardenService.deleteArea(principal.getEmail(), gardenId, areaId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

}