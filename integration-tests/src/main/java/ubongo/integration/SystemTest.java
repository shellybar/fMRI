package ubongo.integration;

import ubongo.common.datatypes.*;
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
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                analysesServer.stop();
            }
        });
        analysesServer = new AnalysesServerImpl(configuration, unitsDirPath, DEBUG);
        analysesServer.start();
        ((AnalysesServerImpl) analysesServer).clearDebugData();
    }


    public static void runTestFlow() throws PersistenceException {
        Unit unit = analysesServer.getAllUnits().get(0);
        unit.setParameterValues("{\"subject\":\"mySubject\"}");

        Context context = new Context("myStudy", "mySubject", "myRuns");
        List<Task> tasks = new ArrayList<>();
        List<Unit> units = new ArrayList<>();

        try {
            tasks.addAll(Task.createTasks(unit, context, 0));
        } catch (Exception e) {
            e.printStackTrace();
        }

        int flowId = analysesServer.createFlow("myStudy", tasks);
        analysesServer.runFlow(flowId);

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Task taskToKill = analysesServer.getTasks(flowId).get(0);
        int idToKill = taskToKill.getId();
        System.out.println("Sending request to kill task id " + idToKill);
        analysesServer.killTask(taskToKill);

        while (true); // TODO change to something nicer
    }

}
