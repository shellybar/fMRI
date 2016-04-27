package ubongo.persistence;

import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;
import ubongo.common.networkUtils.SSHConnectionProperties;
import ubongo.persistence.db.DBConnectionProperties;
import ubongo.persistence.db.DBProxy;

import java.util.Collection;
import java.util.List;

public class PersistenceImpl implements Persistence {

    DBProxy dbProxy;
    UnitFetcher unitFetcher;

    public PersistenceImpl(String unitSettingsDirPath, DBConnectionProperties dbConnectionProperties,
                    SSHConnectionProperties sshConnectionProperties, List<Machine> machines) {
        this(unitSettingsDirPath, dbConnectionProperties, sshConnectionProperties, machines, false);
    }

    public PersistenceImpl(String unitSettingsDirPath,
                           DBConnectionProperties dbConnectionProperties, List<Machine> machines) {
        this(unitSettingsDirPath, dbConnectionProperties, null, machines, false);
    }

    public PersistenceImpl(String unitSettingsDirPath, DBConnectionProperties dbConnectionProperties,
                           SSHConnectionProperties sshConnectionProperties, List<Machine> machines, boolean debug) {
        unitFetcher = new UnitFetcher(unitSettingsDirPath);

        // Database
        if (sshConnectionProperties != null) {
            dbProxy = new DBProxy(unitFetcher, dbConnectionProperties, sshConnectionProperties, machines, debug);
        } else {
            dbProxy = new DBProxy(unitFetcher, dbConnectionProperties, machines, debug);
        }
    }

    @Override
    public void start() throws PersistenceException {
        dbProxy.start();
    }

    @Override
    public void stop() throws PersistenceException {
        dbProxy.disconnect();
    }

    @Override
    public void createAnalysis(String analysisName, List<Unit> units) throws PersistenceException {
        dbProxy.createAnalysis(analysisName, units);
    }

    @Override
    public List<String> getAnalysisNames() throws PersistenceException {
        return dbProxy.getAnalysisNames();
    }

    @Override
    public List<Unit> getAnalysis(String analysisName) throws PersistenceException {
        return dbProxy.getAnalysis(analysisName);
    }

    @Override
    public int createFlow(String studyName, List<Task> tasks) throws PersistenceException {
        return dbProxy.createFlow(studyName, tasks);
    }

    @Override
    public void startFlow(int flowId) throws PersistenceException {
        dbProxy.startFlow(flowId);
    }

    @Override
    public List<Task> cancelFlow(int flowId) {
        return dbProxy.cancelFlow(flowId);
    }

    @Override
    public List<Task> getNewTasks() throws PersistenceException {
        return dbProxy.getNewTasks();
    }

    @Override
    public void updateTaskStatus(Task task) throws PersistenceException {
        dbProxy.updateStatus(task);
    }

    @Override
    public void updateTasksStatus(Collection<Task> waitingTasks) throws PersistenceException {
        dbProxy.updateStatus(waitingTasks);
    }

    @Override
    public List<Task> getTasks(int flowId) throws PersistenceException {
        return dbProxy.getTasks(flowId);
    }

    @Override
    public boolean cancelTask(Task task) throws PersistenceException {
        return dbProxy.cancelTask(task);
    }

    @Override
    public Unit getUnit(int unitId) throws PersistenceException {
        return unitFetcher.getUnit(unitId);
    }

    @Override
    public List<Unit> getAllUnits() throws PersistenceException {
        return unitFetcher.getAllUnits();
    }

    public void clearDebugData() throws PersistenceException {
        dbProxy.clearAllDebugTables();
    }

}
