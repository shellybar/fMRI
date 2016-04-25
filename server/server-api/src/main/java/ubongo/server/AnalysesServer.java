package ubongo.server;

import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;

import java.nio.file.Path;
import java.util.List;

public interface AnalysesServer {

    List<Machine> getAllMachines();

    void runFlow(int flowId);

    void killTask(Task task);

    int createFlow(String studyName, List<Task> tasks);

    void cancelFlow(int flowId);

    List<Task> getTasks(int flowId);

    void cancelTask(Task task);

    List<Unit> getAllUnits();

}
