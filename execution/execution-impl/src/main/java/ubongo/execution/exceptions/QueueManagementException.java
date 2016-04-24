package ubongo.execution.exceptions;

import ubongo.execution.ExecutionException;

public class QueueManagementException extends ExecutionException {

    public QueueManagementException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public QueueManagementException(String message) {
        super(message);
    }
}
