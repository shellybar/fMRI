package edu.tau.eng.neuroscience.mri.dispatcher;


import edu.tau.eng.neuroscience.mri.common.datatypes.Machine;
import edu.tau.eng.neuroscience.mri.common.datatypes.MachineStatistics;
import edu.tau.eng.neuroscience.mri.common.datatypes.Task;

public enum ExecutionProxyImpl implements ExecutionProxy {

    INSTANCE; // This is a singleton

    public static ExecutionProxyImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public void execute(Task task) {

    }

    @Override
    public MachineStatistics getStatistics(Machine machine) {
        return null;
    }

}
