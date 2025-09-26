package integration.model;

public record OrderStatusPayload(Long orderId, Long userId, boolean success, String message) {
}
