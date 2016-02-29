package ubongo.common.exceptions;


public class UnmarshalException extends Exception{
    private String message;

    public UnmarshalException(int code, String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
