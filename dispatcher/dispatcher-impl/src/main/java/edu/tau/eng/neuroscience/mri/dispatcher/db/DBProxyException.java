package edu.tau.eng.neuroscience.mri.dispatcher.db;

import edu.tau.eng.neuroscience.mri.common.exceptions.DispatcherException;

public class DBProxyException extends DispatcherException {

    public DBProxyException(int code, String message) {
        super(code, message);
    }
}
