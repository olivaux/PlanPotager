package eu.planpotager.PlanPotager.user.service;

import eu.planpotager.PlanPotager.user.dao.UserDAO;
import eu.planpotager.PlanPotager.user.dto.UserDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {

    private final UserDAO userDAO;

    public ProfileService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Transactional(readOnly = true)
    public UserDTO getProfile(String userEmail) {
        return userDAO.findByEmail(userEmail)
                .map(user -> new UserDTO(user.getEmail(), user.getUnit(), user.getLanguage(), user.getProviderId(), user.getProvider()))
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));
    }

    public UserDTO updateProfile(String userEmail, String unit, String language) {
        return userDAO.findByEmail(userEmail)
                .map(user -> {
                    user.setUnit(unit);
                    user.setLanguage(language);
                    return new UserDTO(user.getEmail(), user.getUnit(), user.getLanguage(), user.getProviderId(), user.getProvider());
                })
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));
    }

    public void deleteAccount(String userEmail) {
        userDAO.findByEmail(userEmail)
                .ifPresent(userDAO::delete);
    }
}
