package ubongo.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static Logger logger = LogManager.getLogger(AnalysesServerImpl.class);

    private Persistence persistence;
    private Execution execution;
    private boolean debug;

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
        AnalysesServer analysesServer = new AnalysesServerImpl(configuration, unitsDirPath);
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
    public void runFlow(int flowId) {
        execution.runFlow(flowId);
    }

    @Override
    public List<FlowData> getAllFlows(int limit) {
        try {
            return persistence.getAllFlows(limit);
        } catch (PersistenceException e) {
            // TODO handle exception in getAllFlows
            return null;
        }
    }

    @Override
    public void killTask(Task task) {
        execution.killTask(task);
    }

    @Override
    public int createFlow(String studyName, List<Task> tasks) {
        try {
            return persistence.createFlow(studyName, tasks);
        } catch (PersistenceException e) {
            // TODO handle PersistenceException in createFlow
            return 42;
        }
    }

    @Override
    public void cancelFlow(int flowId) {
        try {
            List<Task> tasksToKill = persistence.cancelFlow(flowId);
            tasksToKill.forEach(task -> execution.killTask(task));
        } catch (PersistenceException e) {
            // TODO handle PersistenceException in cancelFlow
        }
    }

    @Override
    public List<Task> getAllTasks(int limit) {
        try {
            return persistence.getAllTasks(limit);
        } catch (PersistenceException e) {
            // TODO handle exception in getAllTasks
            return null;
        }
    }

    @Override
    public List<Task> getTasks(int flowId) {
        try {
            return persistence.getTasks(flowId);
        } catch (PersistenceException e) {
            // TODO handle exception in getTasks
            return null;
        }
    }

    @Override
    public void cancelTask(Task task) {
        try {
            ((ExecutionImpl) execution).notifyQueueBeforeCancel(task);
            if (!persistence.cancelTask(task)) {
                killTask(task); // task could not be canceled - need to be killed
            }
        } catch (PersistenceException e) {
            // TODO handle exception in cancelTask
        } finally {
            ((ExecutionImpl) execution).notifyQueueAfterCancel(task);
        }
    }

    @Override
    public void resumeTask(Task task) {
        try {
            persistence.resumeTask(task);
        } catch (PersistenceException e) {
            // TODO handle exception in resumeTask
        }
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
    public List<Unit> getAllUnits() {
        try {
            return persistence.getAllUnits();
        } catch (PersistenceException e) {
            // TODO handle exception in getAllUnits
            return null;
        }
    }

    public void clearDebugData() {
        try {
            ((PersistenceImpl) persistence).clearDebugData();
        } catch (PersistenceException e) {
            // do nothing - it is only relevant for tests and the exception is already logged
        }
    }
}
