package edu.tau.eng.neuroscience.mri.common.datatypes;


public interface ExecutableObj {

    /**
     * @return a string of inputs from the executable object which will be used for remote execution.
     */
    String getExecutionInputs();

}
