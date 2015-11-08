package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.Machine;
import edu.tau.eng.neuroscience.mri.common.MachineStatistics;
import edu.tau.eng.neuroscience.mri.common.Task;

/**
 * ExecutionProxy is the link between the management layer (i.e. the dispatcher)
 * and the execution layer. It passes requests (HTTP) from the dispatcher to the machines
 * running the different units.
 * Called by the MachinesManager in order to execute a task on the machine itself
 * and calls the MachineController for this purpose.
 */
public interface ExecutionProxy {

    /**
     * Sends an execution request of a unit on a machine (all this information is part of a task).
     * @param task holds within it all the information needed to run a certain unit on a machine
     *             and return the response asynchronously after execution is completed.
     */
    void execute(Task task);

    /**
     * Sends a request to a certain machine to get its latest performance metrics.
     * @param machine is the Machine whose performance statistics are requested.
     * @return MachineStatistics of machine.
     */
    MachineStatistics getStatistics(Machine machine);

}
