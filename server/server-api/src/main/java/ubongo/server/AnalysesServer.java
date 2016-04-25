package ubongo.server;

import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;

import java.nio.file.Path;
import java.util.List;

public interface AnalysesServer {

    List<Machine> getAllMachines();

    void runFlow(long flowId);

    void killTask(Task task);

    long createFlow(String studyName, List<Task> tasks);

    void cancelFlow(long flowId);

    List<Task> getTasks(long flowId);

    void cancelTask(Task task);

    List<Unit> getAllUnits();

}
