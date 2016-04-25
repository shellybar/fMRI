package ubongo.integration;

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
                configuration.getSshConnectionProperties(), false); // TODO change to true for debug!
        persistence.start();
        // TODO clean debug tables in DB - maybe create and then delete
    }

    public static void close() throws PersistenceException {
        persistence.stop();
    }

    public static void createFlow() throws PersistenceException {

        Unit unit = persistence.getUnit(1);
        unit.setParameterValues("{\"srcFile\":\"source_path\", \"destFile\":\"destination_path\"}");

        Task task1 = new Task(0, 0, 0, unit, null,
                TaskStatus.CREATED, "input_path1", "output_path1");
        Task task2 = new Task(0, 0, 0, unit, null,
                TaskStatus.NEW, "input_path2", "output_path2");
        List<Task> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(task2);

        int flowId = persistence.createFlow("study5", tasks); // TODO generate random
        persistence.startFlow(flowId);
        List<Task> returnedTasks = persistence.getNewTasks();
        Task retrievedTask = returnedTasks.get(0);
        assert(retrievedTask.getUnit().getId() == unit.getId()); // TODO assert more things
    }

}