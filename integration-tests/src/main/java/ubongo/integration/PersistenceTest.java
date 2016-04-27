package ubongo.integration;

import ubongo.common.Utils;
import ubongo.common.datatypes.*;
import ubongo.persistence.Persistence;
import ubongo.persistence.PersistenceException;
import ubongo.persistence.PersistenceImpl;
import ubongo.server.Configuration;

import javax.xml.bind.UnmarshalException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PersistenceTest {

    private static final boolean DEBUG = true;
    private static final String CONFIG_PATH = "config";
    private static final String UNITS_DIR_PATH = "units_path";
    private static final String STUDY = "study_2";

    protected Persistence persistence;
    protected Configuration configuration;

    public static void main(String[] args) throws Exception {

        // preparations
        PersistenceTest persistenceTest = new PersistenceTest();
        if (!persistenceTest.init()) return;
        List<Unit> units = persistenceTest.prepareUnits();

        // tests
        units = persistenceTest.createAnalysis(units);
        int flowId = persistenceTest.createFlow(units);
        persistenceTest.startFlow(flowId);
        persistenceTest.close();
    }

    protected boolean init() throws UnmarshalException, PersistenceException {
        String configPath = System.getProperty(CONFIG_PATH);
        String unitsDirPath = System.getProperty(UNITS_DIR_PATH);
        assert configPath != null && unitsDirPath != null;
        configuration = Configuration.loadConfiguration(configPath);
        persistence = new PersistenceImpl(unitsDirPath, configuration.getDbConnectionProperties(),
                configuration.getSshConnectionProperties(), configuration.getMachines(), DEBUG);
        if (!makeDirs()) {
            System.out.println("Failed to create directories for Persistence module test");
            return false;
        }
        persistence.start();
        ((PersistenceImpl) persistence).clearDebugData();
        return true;
    }

    protected void close() throws PersistenceException {
        persistence.stop();
    }

    protected List<Unit> prepareUnits() throws PersistenceException {
        List<Unit> units = new ArrayList<>();
        units.add(persistence.getUnit(88));
        units.add(persistence.getUnit(99));
        return units;
    }

    protected List<Unit> createAnalysis(List<Unit> units) throws PersistenceException {
        String analysis = "analysis1";
        persistence.createAnalysis(analysis, units);
        List<String> analysisNames = persistence.getAnalysisNames();
        assert analysisNames != null && analysisNames.size() == 1;
        String retrievedAnalysisName = analysisNames.get(0);
        assert analysis.equals(retrievedAnalysisName);
        List<Unit> retrievedUnits = persistence.getAnalysis(retrievedAnalysisName);
        assert retrievedUnits != null && retrievedUnits.size() == 2;
        for (int i = 0; i < units.size(); i++) {
            Unit currUnit = units.get(i);
            Unit currRetUnit = retrievedUnits.get(i);
            assert currRetUnit != null && currUnit.getId() == currRetUnit.getId() &&
                    currUnit.getInputPaths().equals(currRetUnit.getInputPaths());
        }
        return retrievedUnits;
    }

    // Important: this method is used by ExecutionTest as well
    protected int createFlow(List<Unit> units) throws Exception {

        Context context = new Context(STUDY, ".*", ".*");
        List<Task> tasks = new ArrayList<>();

        int i = 0;
        for (Unit unit : units) {
            tasks.addAll(Task.createTasks(unit, context, i++));
        }

        int flowId = persistence.createFlow(STUDY, tasks);

        // assertions
        List<Task> returnedTasks = persistence.getTasks(flowId);
        assertReturnedTasks(returnedTasks, TaskStatus.CREATED);
        return flowId;
    }

    private void startFlow(int flowId) throws PersistenceException {
        persistence.startFlow(flowId);
        List<Task> returnedTasks = persistence.getNewTasks();
        assertReturnedTasks(returnedTasks, TaskStatus.NEW);
    }

    private static void assertReturnedTasks(List<Task> returnedTasks, TaskStatus status) {
        String regex = "(.*)\\{(.*)\\}(.*)";
        assert returnedTasks.size() == 6; // 6 total tasks (2 + 4)
        // only two distinct tasks
        assert returnedTasks.stream().map(Task::getSerialNumber).distinct().count() == 2;
        for (Task task : returnedTasks) {
            assert !task.getInputPath().matches(regex); // no variables in input
            assert !task.getOutputPath().matches(regex); // no variables in output
            // subject in context and in params is the same
            task.getUnit().getParameters().forEach(param -> {
                if ("subject".equals(param.getName())) {
                    assert task.getContext().getSubject().equals(param.getValue());
                }
            });
            assert STUDY.equals(task.getContext().getStudy()); // the STUDY is as expected
            assert task.getStatus() == status; // all tasks are in status 'Created'
        }
    }

    private static boolean makeDirs() {
        boolean success = true;
        String base = "C:/Temp/ubongo/studies/";
        outerLoop: for (int i = 1; i < 3; i++) {
            String study = Utils.concatStrings("study_", Integer.toString(i), "/");
            for (int j = 1; j < 3; j++) {
                String subject = Utils.concatStrings(study, "subject_",
                        Integer.toString(i), "_", Integer.toString(j), "/");
                for (int k = 1; k < 3; k++) {
                    String run = Utils.concatStrings(subject, "run_",
                            Integer.toString(i), "_", Integer.toString(j), "_",
                            Integer.toString(k), "/");
                    File file = new File(base, run);
                    if (!(success = file.exists() || file.mkdirs())) {
                        break outerLoop;
                    }
                }
            }
        }
        return success;
    }

}
