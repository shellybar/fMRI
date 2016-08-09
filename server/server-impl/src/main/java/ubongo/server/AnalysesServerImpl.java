package ubongo.server;

import ubongo.common.datatypes.*;

import java.util.List;
import org.apache.commons.cli.*;
import ubongo.execution.Execution;
import ubongo.execution.ExecutionImpl;
import ubongo.persistence.Persistence;
import ubongo.persistence.PersistenceException;
import ubongo.persistence.PersistenceImpl;

import javax.xml.bind.UnmarshalException;

public class AnalysesServerImpl implements AnalysesServer {

    private static final String CONFIG_PATH = "config";
    private static final String UNITS_DIR_PATH = "units_path";

    private Persistence persistence;
    private Execution execution;
    private boolean debug; // TODO use debug field?

    public AnalysesServerImpl(Configuration configuration, String unitSettingsDirPath) {
        this(configuration, unitSettingsDirPath, false);
    }

    public AnalysesServerImpl(Configuration configuration, String unitSettingsDirPath, boolean debug) {
        this.debug = debug;
        persistence = new PersistenceImpl(unitSettingsDirPath,
                configuration.getDbConnectionProperties(), configuration.getSshConnectionProperties(),
                configuration.getMachines(), debug);
        execution = new ExecutionImpl(persistence, configuration.getMachines(), debug);
    }

    // TODO remove main and validateSystemVariables - this is done by the UbongoHTTPServer
    public static void main(String[] args) throws ParseException {

        // initialize analysis server
        String configPath = System.getProperty(CONFIG_PATH);
        String unitsDirPath = System.getProperty(UNITS_DIR_PATH);
        if (validateSystemVariables(configPath, unitsDirPath)) return;
        Configuration configuration;
        try {
            configuration = Configuration.loadConfiguration(configPath);
        } catch (UnmarshalException e) {
            System.out.println(e.getMessage());
            return;
        }
        new AnalysesServerImpl(configuration, unitsDirPath);
    }

    private static boolean validateSystemVariables(String configPath, String unitsDirPath) {
        if (configPath == null || unitsDirPath == null) {
            String pattern = "Please supply %1$s path as run parameter: -%2$s=<path>";
            if (configPath == null) {
                System.out.format(pattern, "configuration", CONFIG_PATH);
            }
            if (unitsDirPath == null) {
                System.out.format(pattern, "units directory", UNITS_DIR_PATH);
            }
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        try {
            persistence.start();
        } catch (PersistenceException e) {
            System.out.println("Failed to start system persistence. Details:\n" + e.getMessage());
        }
        execution.start();
    }

    @Override
    public void stop() {
        execution.stop();
        try {
            persistence.stop();
        } catch (PersistenceException e) {
            // do nothing (the error is already logged in persistence)
        }
    }

    @Override
    public List<Machine> getAllMachines() {
        return execution.getAllMachines();
    }

    @Override
    public void runFlow(int flowId) throws PersistenceException {
        execution.runFlow(flowId);
    }

    @Override
    public List<FlowData> getAllFlows(int limit) throws PersistenceException {
        return persistence.getAllFlows(limit);
    }

    @Override
    public void killTask(Task task) {
        execution.killTask(task);
    }

    @Override
    public int createFlow(String studyName, List<Task> tasks) throws PersistenceException {
        return persistence.createFlow(studyName, tasks);
    }

    @Override
    public void cancelFlow(int flowId) throws PersistenceException {
        List<Task> flowTasks = getTasks(flowId);
        try {
            flowTasks.forEach(((ExecutionImpl) execution)::notifyQueueBeforeCancel);
            List<Task> tasksToKill = persistence.cancelFlow(flowId);
            tasksToKill.forEach(execution::killTask);
        }
        finally {
            flowTasks.forEach(((ExecutionImpl) execution)::notifyQueueAfterCancel);;
        }
    }

    @Override
    public List<Task> getAllTasks(int limit) throws PersistenceException {
        return persistence.getAllTasks(limit);
    }

    @Override
    public List<Task> getTasks(int flowId) throws PersistenceException {
        return persistence.getTasks(flowId);
    }

    @Override
    public Task getTask(int taskId) throws PersistenceException {
        return persistence.getTask(taskId);
    }

    @Override
    public void cancelTask(Task task) throws PersistenceException {
        try {
            ((ExecutionImpl) execution).notifyQueueBeforeCancel(task);
            if (!persistence.cancelTask(task)) {
                killTask(task); // task could not be canceled - need to be killed
            }
        }
        finally {
            ((ExecutionImpl) execution).notifyQueueAfterCancel(task);
        }
    }

    @Override
    public void resumeTask(Task task) throws PersistenceException {
        persistence.resumeTask(task);
    }

    @Override
    public List<String> showTaskLogs(int taskId) {
        return null; // TODO
    }

    @Override
    public List<String> showServerLog() {
        return null; // TODO
    }

    @Override
    public List<Unit> getAllUnits() throws PersistenceException {
        return persistence.getAllUnits();
    }

    public void clearDebugData() {
        try {
            ((PersistenceImpl) persistence).clearDebugData();
        } catch (PersistenceException e) {
            // do nothing - it is only relevant for tests and the exception is already logged
        }
    }
}
