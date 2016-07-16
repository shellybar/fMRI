package ubongo.server;

import ubongo.common.datatypes.FlowData;
import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;

import java.util.List;

public interface AnalysesServer {

    List<Machine> getAllMachines(); // TODO support this with rabbit call to machine

    void runFlow(int flowId);

    // return all flows and studyNames . filter in UI by study name.
    List<FlowData> getAllFlows(int limit);

    void killTask(Task task);

    // Convert units to tasks in javacript (add context)
    int createFlow(String studyName, List<Task> tasks);

    void cancelFlow(int flowId);

    // limit the sql query to 1000 results. add comment in UI. (show all tasks and status to user)
    // enable killing running task, or canceling task.
    List<Task> getAllTasks(int limit);

    List<Task> getTasks(int flowId);

    void cancelTask(Task task);

    // used for re-run a task that has failed (and now on hold), or killed\canceled - or if it is on hold because of previous failed task.
    void resumeTask(Task task);

    // Will show task log from machine combined with server log (with grep on taskId)
    List<String> showTaskLogs(int taskId);

    List<String> showServerLog();

    List<Unit> getAllUnits();

    void start();

    void stop();

}