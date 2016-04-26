package ubongo.integration;

import ubongo.common.datatypes.Context;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.TaskStatus;
import ubongo.common.datatypes.Unit;
import ubongo.persistence.PersistenceException;
import ubongo.server.AnalysesServer;
import ubongo.server.AnalysesServerImpl;
import ubongo.server.Configuration;

import javax.xml.bind.UnmarshalException;
import java.util.ArrayList;
import java.util.List;

public class SystemTest {

    private static final boolean DEBUG = true;
    private static final String CONFIG_PATH = "config";
    private static final String UNITS_DIR_PATH = "units_path";

    private static AnalysesServer analysesServer;

    public static void main(String[] args) throws Exception {
        init();
        runTestFlow();
        close();
    }

    public static void init() throws UnmarshalException, PersistenceException {
        // initialize analysis server
        String configPath = System.getProperty(CONFIG_PATH);
        String unitsDirPath = System.getProperty(UNITS_DIR_PATH);
        Configuration configuration;
        try {
            configuration = Configuration.loadConfiguration(configPath);
        } catch (UnmarshalException e) {
            System.out.println(e.getMessage());
            throw new PersistenceException(e.getMessage());
        }
        analysesServer = new AnalysesServerImpl(configuration, unitsDirPath, DEBUG);
        analysesServer.start();
    }

    public static void close() throws PersistenceException {
        ((AnalysesServerImpl) analysesServer).clearDebugData();
        analysesServer.stop();
    }

    public static void runTestFlow() throws PersistenceException {
        Unit unit = analysesServer.getAllUnits().get(0);
        Task task1 = new Task(0, 0, 0, unit, null,
                TaskStatus.CREATED, new Context("study1", "subject1", null));
        Task task2 = new Task(0, 0, 0, unit, null,
                TaskStatus.NEW, new Context("study1", "subject2", "run"));
        List<Task> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(task2);

        int flowId = analysesServer.createFlow("study1", tasks);
        analysesServer.runFlow(flowId);
    }

}
