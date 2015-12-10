package edu.tau.eng.neuroscience.mri.common.log;

import edu.tau.eng.neuroscience.mri.common.constants.SystemConstants;

import java.io.File;


public class LoggerImpl implements Logger {

    private org.apache.logging.log4j.Logger logger;

    static {
        File logDirFile = new File(SystemConstants.BASE_DIR, "logs");
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
