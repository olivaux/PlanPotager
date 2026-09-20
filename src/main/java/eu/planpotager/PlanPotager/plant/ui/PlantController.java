package eu.planpotager.PlanPotager.plant.ui;

import eu.planpotager.PlanPotager.plant.dto.AddPlantRequest;
import eu.planpotager.PlanPotager.plant.dto.PlantDTO;
import eu.planpotager.PlanPotager.plant.service.PlantService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plant")
public class PlantController {

    private final PlantService plantService;

    public PlantController(PlantService plantService) {
        this.plantService = plantService;
    }

    @PostMapping
    public ResponseEntity<PlantDTO> addPlant(@RequestBody AddPlantRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        PlantDTO plant = plantService.addPlant(request.variety(), request.comment(), principal.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(plant);
    }

    @DeleteMapping("/{plantId}")
    public ResponseEntity<Void> removePlant(@PathVariable Long plantId,
            @AuthenticationPrincipal OidcUser principal) {
        try {
            plantService.removePlant(plantId, principal.getEmail());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("")
    public ResponseEntity<List<PlantDTO>> getAvailablePlants(@AuthenticationPrincipal OidcUser principal) {
        List<PlantDTO> plants = plantService.getAvailablePlants(principal.getEmail());
        return ResponseEntity.ok(plants);
    }

}
