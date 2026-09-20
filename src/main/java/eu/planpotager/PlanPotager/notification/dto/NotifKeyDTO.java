package eu.planpotager.PlanPotager.notification.dto;

// Cle de deduplication d'une notification : un meme message n'est pas renotifie a un meme utilisateur.
public record NotifKeyDTO(String userEmail, String message) {

}
