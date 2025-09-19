package integration.model;

public record UserRegisterPayload(String username, boolean success, String message) {
}
