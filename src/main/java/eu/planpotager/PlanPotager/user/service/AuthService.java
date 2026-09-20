package eu.planpotager.PlanPotager.user.service;

import eu.planpotager.PlanPotager.user.dao.UserDAO;
import eu.planpotager.PlanPotager.user.domain.User;
import eu.planpotager.PlanPotager.user.dto.UserDTO;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Transactional(readOnly = true)
    public Optional<User> checkEmail(String email) {
        return userDAO.findByEmail(email);
    }

    public UserDTO createUser(String email, String provider, String providerId) {
        User user = new User(email);
        user.setProviderId(providerId);
        user.setProvider(provider);
        userDAO.save(user);
        return new UserDTO(user.getEmail(), user.getUnit(), user.getLanguage(), user.getProviderId(), user.getProvider());
    }

    public UserDTO updateProvider(String email, String provider, String providerId) {
        Optional<User> optionalUser = userDAO.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setProviderId(providerId);
            user.setProvider(provider);
            return new UserDTO(user.getEmail(), user.getUnit(), user.getLanguage(), user.getProviderId(), user.getProvider());
        } else {
            throw new RuntimeException("User not found with email: " + email);
        }
    }
}
