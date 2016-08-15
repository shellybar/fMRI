package ubongo.integration;

import com.google.gson.JsonParseException;
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


    public static void runTestFlow() throws PersistenceException, JsonParseException {

        analysesServer.generateBashFileForNewBaseUnit(1);
        analysesServer.generateBashFileForNewBaseUnit(2);

        List<Task> tasks = new ArrayList<>();

        addTasksForUnit1(tasks);
        addTasksForUnit2(tasks);

        int flowId = analysesServer.createFlow("Kinemes", tasks);
        analysesServer.runFlow(flowId);

        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Task taskToKill = analysesServer.getTasks(flowId).get(0);
       // int idToKill = taskToKill.getId();
        //System.out.println("Sending request to kill task id " + idToKill);
       // analysesServer.killTask(taskToKill);

        while (true); // TODO change to something nicer
    }

    private static void addTasksForUnit1(List<Task> tasks) {
        Unit unit1 = null;
        try {
            unit1 = analysesServer.getAllUnits().get(0);
            Context contextUnit1 = new Context("Kinemes", "Subject1", "myRuns");
            tasks.addAll(Task.createTasks(unit1, contextUnit1, 0));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private static void addTasksForUnit2(List<Task> tasks) {
        Unit unit2 = null;
        try {
            unit2 = analysesServer.getAllUnits().get(1);
            unit2.setParameterValues("{\"hicutoff\":\"45\"}");
            Context contextUnit2 = new Context("Kinemes", "Subject1", "myRuns");
            tasks.addAll(Task.createTasks(unit2, contextUnit2, 1));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

}
