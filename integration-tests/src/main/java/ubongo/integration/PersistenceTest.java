package ubongo.integration;

import ubongo.common.datatypes.Context;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.TaskStatus;
import ubongo.common.datatypes.Unit;
import ubongo.persistence.Persistence;
import ubongo.persistence.PersistenceException;
import ubongo.persistence.PersistenceImpl;
import ubongo.server.Configuration;

import javax.xml.bind.UnmarshalException;
import java.util.ArrayList;
import java.util.List;

public class PersistenceTest {

    private static final boolean DEBUG = true;
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
                configuration.getSshConnectionProperties(), configuration.getMachines(), DEBUG);
        persistence.start();
    }

    public static void close() throws PersistenceException {
        ((PersistenceImpl) persistence).clearDebugData();
        persistence.stop();
    }

    public static void createFlow() throws PersistenceException {

        Unit unit = persistence.getUnit(1);
        unit.setParameterValues("{\"srcFile\":\"source_path\", \"destFile\":\"destination_path\"}");

        Task task1 = new Task(0, 0, 0, unit, null,
                TaskStatus.CREATED, new Context("study1", "subject1", null));
        Task task2 = new Task(0, 0, 0, unit, null,
                TaskStatus.NEW, new Context("study1", "subject2", "run"));
        List<Task> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(task2);

        int flowId = persistence.createFlow("study1", tasks);
        persistence.startFlow(flowId);
        List<Task> returnedTasks = persistence.getNewTasks();
        Task retrievedTask = returnedTasks.get(0);
        assert(retrievedTask.getUnit().getId() == unit.getId()); // TODO assert more things
    }

}
