package ubongo.persistence;

import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;
import ubongo.common.networkUtils.SSHConnectionProperties;
import ubongo.persistence.db.DBConnectionProperties;
import ubongo.persistence.db.DBProxy;
import ubongo.persistence.exceptions.DBProxyException;

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
        // TODO call dbProxy
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
    public void cancelFlow(int flowId) {
        // TODO call dbProxy
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
    public List<Task> getTasks(int flowId) throws PersistenceException {
        return null; // TODO call dbProxy
    }

    @Override
    public void cancelTask(Task task) throws PersistenceException {
        // TODO call dbProxy
    }

    @Override
    public Unit getUnit(int unitId) throws PersistenceException {
        return unitFetcher.getUnit(unitId);
    }

    @Override
    public List<Unit> getAllUnits() throws PersistenceException {
        return unitFetcher.getAllUnits();
    }

}
