package ubongo.integration;

import ubongo.common.datatypes.Context;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.TaskStatus;
import ubongo.common.datatypes.Unit;
import ubongo.persistence.Persistence;
import ubongo.persistence.PersistenceException;
import ubongo.persistence.PersistenceImpl;
import ubongo.server.AnalysesServer;
import ubongo.server.AnalysesServerImpl;
import ubongo.server.Configuration;

import javax.xml.bind.UnmarshalException;
import java.util.ArrayList;
import java.util.List;

public class PersistenceTest {

    private static final String CONFIG_PATH = "config";
    private static final String UNITS_DIR_PATH = "units_path";

    private static Persistence persistence;

    public static void main(String[] args) throws Exception {
        init();
        createFlow();
        close();
    }

    public static void init() throws UnmarshalException, PersistenceException {
        String configPath = System.getProperty(CONFIG_PATH);
        String unitsDirPath = System.getProperty(UNITS_DIR_PATH);
        assert(configPath != null && unitsDirPath != null);
        Configuration configuration = Configuration.loadConfiguration(configPath);
        persistence = new PersistenceImpl(unitsDirPath, configuration.getDbConnectionProperties(),
                configuration.getSshConnectionProperties(), configuration.getMachines(), false); // TODO change to true for debug!
        persistence.start();
        // TODO clean debug tables in DB - maybe create and then delete
        initializeAnalysisServer();
    }

    public static void close() throws PersistenceException {
        persistence.stop();
    }

    public static void createFlow() throws PersistenceException {
        Unit unit = persistence.getUnit(1);
        unit.setParameterValues("{\"srcFile\":\"source_path\", \"destFile\":\"destination_path\"}");

        Task task1 = new Task(0, 0, 0, unit, null,
                TaskStatus.CREATED, new Context("study1", "subject1", null));
        Task task2 = new Task(0, 0, 0, unit, null,
                TaskStatus.NEW, new Context("study2", "subject2", "run"));
        List<Task> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(task2);

        int flowId = persistence.createFlow("testFlow3", tasks); // TODO generate random
        persistence.startFlow(flowId);
        List<Task> returnedTasks = persistence.getNewTasks();
        Task retrievedTask = returnedTasks.get(0);
        assert(retrievedTask.getUnit().getId() == unit.getId()); // TODO assert more things
    }

    public static void initializeAnalysisServer() throws PersistenceException {
        // initialize analysis server
        String configPath = System.getProperty(CONFIG_PATH);
        String unitsDirPath = System.getProperty(UNITS_DIR_PATH);
        if (validateSystemVariables(configPath, unitsDirPath)) return;
        Configuration configuration;
        try {
            configuration = Configuration.loadConfiguration(configPath);
        } catch (UnmarshalException e) {
            System.out.println(e.getMessage());
            throw new PersistenceException(e.getMessage());
        }
        AnalysesServer analysesServer = new AnalysesServerImpl(configuration, unitsDirPath);
        analysesServer.start();
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

}
