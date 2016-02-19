package ubongo.common.exceptions;

/**
 * An Exception class for exceptions stemming from any dispatcher related problems
 * (e.g., invalid flow received from user, failure to send execution request to machine).
 */
public class DispatcherException extends Exception {

    private int code;
    private String message;

    public DispatcherException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
