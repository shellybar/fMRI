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
        Task task1 = new Task(0, unit, new Context("myStudy", "mySubject", "myRun"));
        List<Task> tasks = new ArrayList<>();
        tasks.add(task1);

        int flowId = analysesServer.createFlow("myStudy", tasks);
        analysesServer.runFlow(flowId);
        while (true); // TODO change to something nicer
    }

}
