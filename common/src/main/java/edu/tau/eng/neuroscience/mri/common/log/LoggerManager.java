package edu.tau.eng.neuroscience.mri.common.log;

public class LoggerManager {

    public static Logger getLogger(Class c) {
        // TODO add Thread Local Map for contextual information such as requestId, user, etc. (see: MDC)
        return new LoggerImpl(c);
    }

}
