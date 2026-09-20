package eu.planpotager.PlanPotager.plant.ui;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import eu.planpotager.PlanPotager.config.SecurityConfig;
import eu.planpotager.PlanPotager.plant.dto.AddPlantRequest;
import eu.planpotager.PlanPotager.plant.dto.PlantDTO;
import eu.planpotager.PlanPotager.plant.service.PlantService;
import eu.planpotager.PlanPotager.user.service.CustomOidcUserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlantController.class)
@Import(SecurityConfig.class)
class PlantControllerTest {

    private static final String EMAIL = "jane.doe@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomOidcUserService customOidcUserService;

    @MockitoBean
    private PlantService plantService;

    @Test
    void addPlant_shouldReturnCreatedPlant_whenAuthenticated() throws Exception {
        PlantDTO created = new PlantDTO(1L, "Tomate Cerise", "Graines du Midi", "Tomate", 0.2);
        when(plantService.addPlant("Tomate Cerise", "Graines du Midi", EMAIL)).thenReturn(created);

        mockMvc.perform(post("/api/plant")
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddPlantRequest("Tomate Cerise", "Graines du Midi"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.variety").value("Tomate Cerise"))
                .andExpect(jsonPath("$.comment").value("Graines du Midi"));
    }

    @Test
    void addPlant_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/plant")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddPlantRequest("Tomate Cerise", "Graines du Midi"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void removePlant_shouldReturnNoContent_whenOwnedByUser() throws Exception {
        mockMvc.perform(delete("/api/plant/{plantId}", 1L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void removePlant_shouldReturnNotFound_whenPlantDoesNotBelongToUserOrDoesNotExist() throws Exception {
        doThrow(new IllegalArgumentException("Plant not found"))
                .when(plantService).removePlant(1L, EMAIL);

        mockMvc.perform(delete("/api/plant/{plantId}", 1L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void removePlant_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/plant/{plantId}", 1L).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAvailablePlants_shouldReturnPlantsOfUser_whenAuthenticated() throws Exception {
        PlantDTO tomato = new PlantDTO(1L, "Tomate Cerise", "Graines du Midi", "Tomate", 0.2);
        when(plantService.getAvailablePlants(EMAIL)).thenReturn(List.of(tomato));

        mockMvc.perform(get("/api/plant")
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].variety").value("Tomate Cerise"));
    }

    @Test
    void getAvailablePlants_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/plant"))
                .andExpect(status().isUnauthorized());
    }
}
