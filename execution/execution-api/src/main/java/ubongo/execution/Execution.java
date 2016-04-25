package ubongo.execution;

import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.Task;

import java.util.List;

public interface Execution {

    void start();

    void stop();

    void runFlow(int flowId);

    void killTask(Task task);

    List<Machine> getAllMachines();

}
