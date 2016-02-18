package edu.tau.eng.neuroscience.mri.common.exceptions;

public class QueueManagementException extends DispatcherException {
    public QueueManagementException(int code, String message) {
        super(code, message);
    }
}
