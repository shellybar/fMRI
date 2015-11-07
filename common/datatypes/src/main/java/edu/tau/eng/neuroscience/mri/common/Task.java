package edu.tau.eng.neuroscience.mri.common;

/**
 * A task encapsulates all the information to run a unit on a machine
 * and return a response once the execution has completed.
 */
public interface Task {

    /**
     * @return a unique task ID as it is saved in the DB.
     */
    int getTaskId();

    /**
     * @return the unit that is required to run in this task.
     */
    Unit getUnit();

    /**
     * @return the machine on which to run the unit.
     */
    Machine getMachine();

}
