package ubongo.dispatcher.db;

import ubongo.common.exceptions.DispatcherException;

public class DBProxyException extends DispatcherException {

    public DBProxyException(int code, String message) {
        super(code, message);
    }
}
