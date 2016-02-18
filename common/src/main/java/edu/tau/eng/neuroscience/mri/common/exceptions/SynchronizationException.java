package edu.tau.eng.neuroscience.mri.common.exceptions;

public class SynchronizationException extends DispatcherException {
    public SynchronizationException(int code, String message) {
        super(code, message);
    }
}
