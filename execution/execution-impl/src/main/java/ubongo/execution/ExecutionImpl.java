package ubongo.execution;

import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.Task;
import ubongo.execution.exceptions.QueueManagementException;
import ubongo.persistence.Persistence;

import java.util.List;

public class ExecutionImpl implements Execution {

    private Persistence persistence;
    private MachinesManager machinesManager;
    private QueueManager queueManager;
    private ExecutionProxy executionProxy;

    public ExecutionImpl(Persistence persistence, List<Machine> machines) {
        this.persistence = persistence;
        executionProxy = ExecutionProxy.getInstance();
        machinesManager = new MachinesManager(machines, executionProxy);
        queueManager = new QueueManager(persistence, machinesManager);
    }

    @Override
    public void start() {
        try {
            machinesManager.start();
            queueManager.start();
        } catch (QueueManagementException e) {
            // TODO handle QueueManagementException on dispatcher start
        }
    }

    @Override
    public void stop() {
        queueManager.stop();
        machinesManager.stop();
    }

    @Override
    public void runFlow(long flowId) {
        persistence.startFlow(flowId);
    }

    @Override
    public void killTask(Task task) {
        executionProxy.killTask(task);
    }

    @Override
    public List<Machine> getAllMachines() {
        return machinesManager.getAllMachines();
    }

    /**
     * A call to this function notifies the Execution that a fatal error has occurred
     */
    protected static void notifyFatal(Throwable e) {
        // TODO notify UI, try to solve based on type of error...
    }
}
