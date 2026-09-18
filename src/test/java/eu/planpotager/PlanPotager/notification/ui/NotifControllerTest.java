package eu.planpotager.PlanPotager.notification.ui;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.planpotager.PlanPotager.config.SecurityConfig;
import eu.planpotager.PlanPotager.notification.dto.NotifDTO;
import eu.planpotager.PlanPotager.notification.service.NotifService;
import eu.planpotager.PlanPotager.user.service.CustomOidcUserService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotifController.class)
@Import(SecurityConfig.class)
class NotifControllerTest {

    private static final String EMAIL = "jane.doe@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomOidcUserService customOidcUserService;

    @MockitoBean
    private NotifService notifService;

    @Test
    void getNotifications_shouldReturnUserNotifications_whenAuthenticated() throws Exception {
        NotifDTO notif = new NotifDTO(1L, "Plante Tomate à Planter", "A_PLANTER", false, LocalDateTime.now());
        when(notifService.getNotificationsByUser(EMAIL)).thenReturn(List.of(notif));

        mockMvc.perform(get("/api/notif")
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("Plante Tomate à Planter"));
    }

    @Test
    void getNotifications_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/notif"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readNotif_shouldReturnNoContent_whenNotificationExists() throws Exception {
        mockMvc.perform(put("/api/notif/{id}/read", 1L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void readNotif_shouldReturnNotFound_whenNotificationDoesNotExist() throws Exception {
        doThrow(new IllegalArgumentException("Notification not found")).when(notifService).setNotifAsRead(1L);

        mockMvc.perform(put("/api/notif/{id}/read", 1L)
                .with(oidcLogin().userInfoToken(token -> token.claim("email", EMAIL)))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void readNotif_shouldBeRejected_whenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/notif/{id}/read", 1L)
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
