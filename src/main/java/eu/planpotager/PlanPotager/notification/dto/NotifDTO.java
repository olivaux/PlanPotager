package eu.planpotager.PlanPotager.notification.dto;

import java.time.LocalDateTime;

public record NotifDTO(Long id, String message, String type, Boolean isRead, LocalDateTime createdAt) {

}
