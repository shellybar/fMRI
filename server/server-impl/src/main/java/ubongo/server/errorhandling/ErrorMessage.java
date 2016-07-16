package ubongo.server.errorhandling;

public class ErrorMessage {

    private int status;
    private String message;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ErrorMessage(UbongoHttpException ex) {
        this.status = ex.getStatus();
        this.message = ex.getMessage();
    }

    public ErrorMessage() {}

    public String toJsonString() {
        return "{\"status\": \"" + status + "\", \"message\": \"" + message + "\"}";
    }
}