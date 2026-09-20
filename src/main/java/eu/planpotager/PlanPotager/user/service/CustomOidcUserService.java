package eu.planpotager.PlanPotager.user.service;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Optional;

import eu.planpotager.PlanPotager.user.domain.User;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final AuthService authService;

    public CustomOidcUserService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        OidcUser oidcUser = super.loadUser(userRequest);

        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            throw new OAuth2AuthenticationException("Email not verified by provider");
        }

        String email = oidcUser.getEmail();
        String providerId = oidcUser.getSubject();
        String provider = userRequest.getClientRegistration().getRegistrationId();

        try {
            Optional<User> existingUser = authService.checkEmail(email);
            if (existingUser.isPresent()) {
                authService.updateProvider(email, provider, providerId);
            } else {
                authService.createUser(email, provider, providerId);
            }
        } catch (RuntimeException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("server_error", "Failed to load or create user", null), e);
        }

        return oidcUser;
    }

}
