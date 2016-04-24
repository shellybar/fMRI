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
import ubongo.persistence.PersistenceImpl;

public class AnalysesServerImpl implements AnalysesServer {

    private static final String CONFIG_PATH = "config";
    private static final String UNITS_DIR_PATH = "units_path";

    private static Logger logger = LogManager.getLogger(AnalysesServerImpl.class);

    Persistence persistence;
    Execution execution;

    private AnalysesServerImpl() {
        persistence = new PersistenceImpl();
        execution = new ExecutionImpl();
    }

    private void start() {
        persistence.start();
        execution.start();
    }

    private void stop() {
        execution.stop();
        persistence.stop();
    }

    public static void main(String[] args) throws ParseException {

        String configPath = System.getProperty(CONFIG_PATH);
        String unitsDirPath = System.getProperty(UNITS_DIR_PATH);

        // TODO load configuration

        AnalysesServer analysesServer = new AnalysesServerImpl();

        ExecutionProxy executionProxy = ExecutionProxy.getInstance();
        QueueManager queueManager = new QueueManager();


        int id = 1;
        Machine machine = new Machine();
        machine.setPort(1234);
        machine.setAddress("rack-hezi-01");
        String inputPath= "/specific/a/home/cc/students/cs/razregev/workspace/fmri/rabbitTests/unit7Inputs";
        String outputPath = "/specific/a/home/cc/students/cs/razregev/workspace/fmri/rabbitTests/unit7Outputs";
        Unit unit = new BaseUnit(7);


        Task taskToExec = new Task(id, unit, machine, TaskStatus.PROCESSING, inputPath, outputPath);
        executionProxy.execute(taskToExec,queueManager);
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

    @Override
    public List<Machine> getAllMachines() {
        return null;
    }

    @Override
    public void runFlow(long flowId) {

    }

    @Override
    public void killTask(Task task) {

    }

    @Override
    public long createFlow(String studyName, Path studyRootDir) {
        return 0;
    }

    @Override
    public void cancelFlow(long flowId) {

    }

    @Override
    public void addTasks(List<Task> tasks) {

    }

    @Override
    public List<Task> getTasks(long flowId) {
        return null;
    }

    @Override
    public void cancelTask(Task task) {

    }

    @Override
    public List<Unit> getAllUnits() {
        return null;
    }
}
