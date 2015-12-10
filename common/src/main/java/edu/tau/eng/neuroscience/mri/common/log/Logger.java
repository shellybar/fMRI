package edu.tau.eng.neuroscience.mri.common.log;


public interface Logger {

    void debug(String message);

    void info(String message);

    void warn(String message);

    void error(String message);

    void fatal(String message);

}
