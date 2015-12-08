package edu.tau.eng.neuroscience.mri.common;

import java.io.File;

public class LoggerImpl implements Logger {

    private org.apache.logging.log4j.Logger logger;

    static {
        String logDir = System.getenv("LOG_DIR");
        File logDirFile = (logDir == null)? new File(System.getenv("TEMP"), "mri/logs") : new File(logDir);
        if (System.getProperty("log.directory") == null) {
            System.setProperty("log.directory", logDirFile.getAbsolutePath());
        }
    }

    protected LoggerImpl(Class c) {
        logger = org.apache.logging.log4j.LogManager.getLogger(c);
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void fatal(String message) {
        logger.fatal(message);
    }

}
