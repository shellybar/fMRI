package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.Machine;
import edu.tau.eng.neuroscience.mri.common.MachineStatistics;
import edu.tau.eng.neuroscience.mri.common.Task;

/**
 * Created by regevr on 11/1/2015.
 */
public interface ExecutionProxy {

    void execute(Task task);
    MachineStatistics getStatistics(Machine machine);

}
