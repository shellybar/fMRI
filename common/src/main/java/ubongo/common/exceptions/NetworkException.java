package ubongo.common.exceptions;


public class NetworkException extends Exception{
    private String message;

    public NetworkException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
