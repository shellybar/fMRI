package ubongo.integration;

import ubongo.common.datatypes.Context;
import ubongo.common.datatypes.Task;
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
        int flowId = createFlow();
        startFlow(flowId);
        close();
    }

    private static void init() throws UnmarshalException, PersistenceException {
        String configPath = System.getProperty(CONFIG_PATH);
        String unitsDirPath = System.getProperty(UNITS_DIR_PATH);
        assert(configPath != null && unitsDirPath != null);
        Configuration configuration = Configuration.loadConfiguration(configPath);
        persistence = new PersistenceImpl(unitsDirPath, configuration.getDbConnectionProperties(),
                configuration.getSshConnectionProperties(), configuration.getMachines(), DEBUG);
        // TODO create temp folders
        persistence.start();
        ((PersistenceImpl) persistence).clearDebugData();
    }

    private static void close() throws PersistenceException {
        persistence.stop();
        // TODO delete temp folders
    }

    private static int createFlow() throws Exception {

        Context context = new Context("study_2", ".*", ".*");
        List<Task> tasks = new ArrayList<>();
        List<Unit> units = new ArrayList<>();
        units.add(persistence.getUnit(88));
        units.add(persistence.getUnit(99));

        int i = 0;
        for (Unit unit : units) {
            tasks.addAll(Task.createTasks(unit, context, i++));
        }

        int flowId = persistence.createFlow("study_2", tasks);
        List<Task> returnedTasks = persistence.getTasks(flowId);
        // TODO assert
        return flowId;
    }

    private static void startFlow(int flowId) throws PersistenceException {
        persistence.startFlow(flowId);
        List<Task> returnedTasks = persistence.getNewTasks();
        Task retrievedTask = returnedTasks.get(0);
        // TODO assert
    }

}
