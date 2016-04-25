package ubongo.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.datatypes.*;

import java.nio.file.Path;
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

    Persistence persistence;
    Execution execution;

    private AnalysesServerImpl(Configuration configuration, String unitSettingsDirPath) {
        persistence = new PersistenceImpl(unitSettingsDirPath,
                configuration.getDbConnectionProperties(), configuration.getSshConnectionProperties());
        execution = new ExecutionImpl(persistence, configuration.getMachines());
    }

    public static void main(String[] args) throws ParseException {

        // initialize analysis server
        String configPath = System.getProperty(CONFIG_PATH);
        String unitsDirPath = System.getProperty(UNITS_DIR_PATH);
        if (validateSystemVariables(configPath, unitsDirPath)) return;
        Configuration configuration;
        try {
            configuration = Configuration.loadConfiguration(CONFIG_PATH);
        } catch (UnmarshalException e) {
            System.out.println(e.getMessage());
            return;
        }
        AnalysesServer analysesServer = new AnalysesServerImpl(configuration, unitsDirPath);

        int id = 1;
        Machine machine = new Machine();
        machine.setPort(1234);
        machine.setAddress("rack-hezi-01");
        String inputPath= "/specific/a/home/cc/students/cs/razregev/workspace/fmri/rabbitTests/unit7Inputs";
        String outputPath = "/specific/a/home/cc/students/cs/razregev/workspace/fmri/rabbitTests/unit7Outputs";
        Unit unit = new BaseUnit(7);

        Task taskToExec = new Task(id, unit, machine, TaskStatus.PROCESSING, inputPath, outputPath);
//        executionProxy.execute(taskToExec,queueManager);
//        logger.info("Start...");
//        Properties props = parseCommandLineArgs(args);
//        Execution dispatcher = new ExecutionImpl(props);
//        dispatcher.start();
//
//        Unit unit = new BaseUnit();
//        unit.setId(1);
//        unit.setParameterValues("{}"); // TODO unit.setInputPath("serverWorkspace"); change this protocol - pass a task
//        dispatcher.run(null, unit);
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

    private void start() {
        try {
            persistence.start();
        } catch (PersistenceException e) {
            System.out.println("Failed to start system persistence. Details:\n" + e.getMessage());
        }
        execution.start();
    }

    private void stop() {
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
    public void runFlow(long flowId) {
        execution.runFlow(flowId);
    }

    @Override
    public void killTask(Task task) {
        execution.killTask(task);
    }

    @Override
    public long createFlow(String studyName, List<Task> tasks) {
        try {
            return persistence.createFlow(studyName, tasks);
        } catch (PersistenceException e) {
            // TODO handle exception in createFlow
            return 42;
        }
    }

    @Override
    public void cancelFlow(long flowId) {
        persistence.cancelFlow(flowId);
    }

    @Override
    public List<Task> getTasks(long flowId) {
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
            persistence.cancelTask(task);
        } catch (PersistenceException e) {
            // TODO handle exception in cancelTask
        }
    }

    @Override
    public List<Unit> getAllUnits() {
        try {
            return persistence.getAllUnits();
        } catch (PersistenceException e) {
            // TODO handle exception in getAllUnits
        }
    }
}
