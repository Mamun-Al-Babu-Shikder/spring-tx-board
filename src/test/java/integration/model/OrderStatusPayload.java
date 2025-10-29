package integration.model;

public class OrderStatusPayload {
    private Long orderId;
    private Long userId;
    private boolean success;
    private String message;

    public OrderStatusPayload(Long orderId, Long userId, boolean success, String message) {
        this.orderId = orderId;
        this.userId = userId;
        this.success = success;
        this.message = message;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
