package ubongo.execution.exceptions;

import ubongo.execution.ExecutionException;

public class MachinesManagementException extends ExecutionException {

    public MachinesManagementException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public MachinesManagementException(String message) {
        super(message);
    }
}