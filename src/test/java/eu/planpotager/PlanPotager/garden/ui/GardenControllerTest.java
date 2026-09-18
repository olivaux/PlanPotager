package eu.planpotager.PlanPotager.garden.ui;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import eu.planpotager.PlanPotager.config.SecurityConfig;
import eu.planpotager.PlanPotager.garden.domain.PlantState;
import eu.planpotager.PlanPotager.garden.dto.AddPlantToGardenRequest;
import eu.planpotager.PlanPotager.garden.dto.AreaDTO;
import eu.planpotager.PlanPotager.garden.dto.GardenDTO;
import eu.planpotager.PlanPotager.garden.dto.GardenPlantDTO;
import eu.planpotager.PlanPotager.garden.dto.GardenRequest;
import eu.planpotager.PlanPotager.garden.dto.SetStateRequest;
import eu.planpotager.PlanPotager.garden.service.GardenService;
import eu.planpotager.PlanPotager.user.service.CustomOidcUserService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GardenController.class)
@Import(SecurityConfig.class)
class GardenControllerTest {

    private static final String EMAIL = "jane.doe@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomOidcUserService customOidcUserService;

    @MockitoBean
    private GardenService gardenService;

    @Test
    void createGarden_shouldReturnCreatedGarden_whenAuthenticated() throws Exception {
        GardenDTO created = new GardenDTO(1L, "Potager du fond", 2.35, 48.85);
        when(gardenService.createGarden("Potager du fond", 2.35, 48.85, EMAIL)).thenReturn(created);

        mockMvc.perform(post("/api/garden")
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GardenRequest("Potager du fond", 2.35, 48.85))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Potager du fond"));
    }

    @Test
    void createGarden_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/garden")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GardenRequest("Potager du fond", 2.35, 48.85))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getGardensByUser_shouldReturnGardensOfUser_whenAuthenticated() throws Exception {
        GardenDTO garden = new GardenDTO(1L, "Potager du fond", 2.35, 48.85);
        when(gardenService.getGardensByUser(EMAIL)).thenReturn(List.of(garden));

        mockMvc.perform(get("/api/garden")
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Potager du fond"));
    }

    @Test
    void getGardensByUser_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/garden"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getGardenById_shouldReturnGarden_whenAuthenticated() throws Exception {
        GardenDTO garden = new GardenDTO(1L, "Potager du fond", 2.35, 48.85);
        when(gardenService.getGardenById(EMAIL, 1L)).thenReturn(garden);

        mockMvc.perform(get("/api/garden/{id}", 1L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Potager du fond"));
    }

    @Test
    void getGardenById_shouldReturnNotFound_whenGardenDoesNotBelongToUserOrDoesNotExist() throws Exception {
        when(gardenService.getGardenById(EMAIL, 1L))
                .thenThrow(new IllegalArgumentException("Garden not found"));

        mockMvc.perform(get("/api/garden/{id}", 1L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getGardenById_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/garden/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateGarden_shouldReturnUpdatedGarden_whenAuthenticated() throws Exception {
        GardenDTO updated = new GardenDTO(1L, "Nouveau nom", 2.36, 48.86);
        when(gardenService.updateGarden(EMAIL, 1L, "Nouveau nom", 2.36, 48.86)).thenReturn(updated);

        mockMvc.perform(put("/api/garden/{id}", 1L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GardenRequest("Nouveau nom", 2.36, 48.86))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nouveau nom"));
    }

    @Test
    void updateGarden_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/garden/{id}", 1L)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GardenRequest("Nouveau nom", 2.36, 48.86))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteGarden_shouldReturnNoContent_whenAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/garden/{id}", 1L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteGarden_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/garden/{id}", 1L).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addPlantToGarden_shouldReturnCreatedGardenPlant_whenAuthenticated() throws Exception {
        GardenPlantDTO created = new GardenPlantDTO(7L, 10, 20, PlantState.A_PLANTER, 42L);
        when(gardenService.addPlantToGarden(EMAIL, 1L, 42L, 10, 20)).thenReturn(created);

        mockMvc.perform(post("/api/garden/{gardenId}/plant", 1L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddPlantToGardenRequest(42L, 10, 20))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.plantId").value(42));
    }

    @Test
    void addPlantToGarden_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/garden/{gardenId}/plant", 1L)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddPlantToGardenRequest(42L, 10, 20))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPlantCurrentPosition_shouldReturnPosition_whenAuthenticated() throws Exception {
        GardenPlantDTO position = new GardenPlantDTO(7L, 10, 20, PlantState.PLANTEE, 42L);
        when(gardenService.getPlantCurrentPosition(EMAIL, 1L, 42L)).thenReturn(position);

        mockMvc.perform(get("/api/garden/{gardenId}/plant/{plantId}", 1L, 42L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.x").value(10))
                .andExpect(jsonPath("$.y").value(20));
    }

    @Test
    void getPlantCurrentPosition_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/garden/{gardenId}/plant/{plantId}", 1L, 42L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePlantPosition_shouldReturnUpdatedGarden_whenAuthenticated() throws Exception {
        GardenDTO garden = new GardenDTO(1L, "Potager du fond", 2.35, 48.85);
        when(gardenService.changePlantPosition(EMAIL, 1L, 42L, 30, 40)).thenReturn(garden);

        mockMvc.perform(put("/api/garden/{gardenId}/plant/{plantId}/position", 1L, 42L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("x", 30, "y", 40))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void changePlantPosition_shouldReturnConflict_whenPlantAlreadyPlanted() throws Exception {
        when(gardenService.changePlantPosition(EMAIL, 1L, 42L, 30, 40))
                .thenThrow(new IllegalStateException("Plant position cannot change once planted"));

        mockMvc.perform(put("/api/garden/{gardenId}/plant/{plantId}/position", 1L, 42L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("x", 30, "y", 40))))
                .andExpect(status().isConflict());
    }

    @Test
    void changePlantPosition_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/garden/{gardenId}/plant/{plantId}/position", 1L, 42L)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("x", 30, "y", 40))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAvailableStates_shouldReturnAllStates_whenAuthenticated() throws Exception {
        when(gardenService.getAvailableStates()).thenReturn(List.of(PlantState.values()));

        mockMvc.perform(get("/api/garden/{gardenId}/plant/{plantId}/states", 1L, 42L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void getAvailableStates_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/garden/{gardenId}/plant/{plantId}/states", 1L, 42L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void setPlantState_shouldReturnUpdatedGarden_whenAuthenticated() throws Exception {
        GardenDTO garden = new GardenDTO(1L, "Potager du fond", 2.35, 48.85);
        when(gardenService.setPlantState(EMAIL, 1L, 42L, PlantState.RECOLTEE)).thenReturn(garden);

        mockMvc.perform(put("/api/garden/{gardenId}/plant/{plantId}/state", 1L, 42L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SetStateRequest(PlantState.RECOLTEE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void setPlantState_shouldReturnConflict_whenTransitionSkipsAState() throws Exception {
        when(gardenService.setPlantState(EMAIL, 1L, 42L, PlantState.RECOLTEE))
                .thenThrow(new IllegalStateException("Cannot transition plant from A_PLANTER to RECOLTEE"));

        mockMvc.perform(put("/api/garden/{gardenId}/plant/{plantId}/state", 1L, 42L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SetStateRequest(PlantState.RECOLTEE))))
                .andExpect(status().isConflict());
    }

    @Test
    void setPlantState_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/garden/{gardenId}/plant/{plantId}/state", 1L, 42L)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SetStateRequest(PlantState.RECOLTEE))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void removePlantFromGarden_shouldReturnUpdatedGarden_whenAuthenticated() throws Exception {
        GardenDTO garden = new GardenDTO(1L, "Potager du fond", 2.35, 48.85);
        when(gardenService.removePlantFromGarden(EMAIL, 1L, 42L)).thenReturn(garden);

        mockMvc.perform(delete("/api/garden/{gardenId}/plant/{plantId}", 1L, 42L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void removePlantFromGarden_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/garden/{gardenId}/plant/{plantId}", 1L, 42L).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addArea_shouldReturnCreatedArea_whenAuthenticated() throws Exception {
        AreaDTO points = new AreaDTO(null, 0.0, 10.0, 10.0, 10.0, 10.0, 0.0, 0.0, 0.0);
        AreaDTO created = new AreaDTO(5L, 0.0, 10.0, 10.0, 10.0, 10.0, 0.0, 0.0, 0.0);
        when(gardenService.addArea(EMAIL, 1L, points)).thenReturn(created);

        mockMvc.perform(post("/api/garden/{gardenId}/area", 1L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(points)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void addArea_shouldBeRejected_whenNotAuthenticated() throws Exception {
        AreaDTO points = new AreaDTO(null, 0.0, 10.0, 10.0, 10.0, 10.0, 0.0, 0.0, 0.0);

        mockMvc.perform(post("/api/garden/{gardenId}/area", 1L)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(points)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateArea_shouldReturnUpdatedArea_whenAuthenticated() throws Exception {
        AreaDTO points = new AreaDTO(5L, 1.0, 1.0, 11.0, 1.0, 11.0, 11.0, 1.0, 11.0);
        when(gardenService.updateArea(EMAIL, 1L, 5L, points)).thenReturn(points);

        mockMvc.perform(put("/api/garden/{gardenId}/area/{areaId}", 1L, 5L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(points)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leftUpX").value(1.0));
    }

    @Test
    void updateArea_shouldBeRejected_whenNotAuthenticated() throws Exception {
        AreaDTO points = new AreaDTO(5L, 1.0, 1.0, 11.0, 1.0, 11.0, 11.0, 1.0, 11.0);

        mockMvc.perform(put("/api/garden/{gardenId}/area/{areaId}", 1L, 5L)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(points)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteArea_shouldReturnNoContent_whenAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/garden/{gardenId}/area/{areaId}", 1L, 5L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteArea_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/garden/{gardenId}/area/{areaId}", 1L, 5L).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getGardenPlants_shouldReturnPlants_whenAuthenticated() throws Exception {
        GardenPlantDTO plant = new GardenPlantDTO(7L, 10, 20, PlantState.PLANTEE, 42L);
        when(gardenService.getGardenPlants(EMAIL, 1L)).thenReturn(List.of(plant));

        mockMvc.perform(get("/api/garden/{gardenId}/plants", 1L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].x").value(10));
    }

    @Test
    void getGardenPlants_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/garden/{gardenId}/plants", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getGardenAreas_shouldReturnAreas_whenAuthenticated() throws Exception {
        AreaDTO area = new AreaDTO(5L, 0.0, 10.0, 10.0, 10.0, 10.0, 0.0, 0.0, 0.0);
        when(gardenService.getGardenAreas(EMAIL, 1L)).thenReturn(List.of(area));

        mockMvc.perform(get("/api/garden/{gardenId}/areas", 1L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void getGardenAreas_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/garden/{gardenId}/areas", 1L))
                .andExpect(status().isUnauthorized());
    }
}