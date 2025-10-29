package integration.model;

public class UserRegisterPayload {
    private String username;
    private boolean success;
    private String message;

    public UserRegisterPayload(String username, boolean success, String message) {
        this.username = username;
        this.success = success;
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
