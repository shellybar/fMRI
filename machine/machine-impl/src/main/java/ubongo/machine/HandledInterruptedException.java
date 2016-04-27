package ubongo.machine;

public class HandledInterruptedException extends Exception {
    public HandledInterruptedException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public HandledInterruptedException(String message) {
        super(message);
    }
}
