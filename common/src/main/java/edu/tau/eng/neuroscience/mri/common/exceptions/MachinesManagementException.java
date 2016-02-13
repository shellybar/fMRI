package edu.tau.eng.neuroscience.mri.common.exceptions;

public class MachinesManagementException extends DispatcherException {
    public MachinesManagementException(int code, String message) {
        super(code, message);
    }
}