package ubongo.persistence;

import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;
import ubongo.common.networkUtils.SSHConnectionProperties;
import ubongo.persistence.db.DBConnectionProperties;
import ubongo.persistence.db.DBProxy;

import java.util.List;

public class PersistenceImpl implements Persistence {

    DBProxy dbProxy;
    UnitFetcher unitFetcher;

    public PersistenceImpl(String unitSettingsDirPath, DBConnectionProperties dbConnectionProperties,
                    SSHConnectionProperties sshConnectionProperties) {
        this(unitSettingsDirPath, dbConnectionProperties, sshConnectionProperties, false);
    }

    public PersistenceImpl(String unitSettingsDirPath, DBConnectionProperties dbConnectionProperties) {
        this(unitSettingsDirPath, dbConnectionProperties, null, false);
    }

    public PersistenceImpl(String unitSettingsDirPath, DBConnectionProperties dbConnectionProperties,
                    SSHConnectionProperties sshConnectionProperties, boolean debug) {
        unitFetcher = new UnitFetcher(unitSettingsDirPath);

        // Database
        if (sshConnectionProperties != null) {
            dbProxy = new DBProxy(unitFetcher, dbConnectionProperties, sshConnectionProperties, debug);
        } else {
            dbProxy = new DBProxy(unitFetcher, dbConnectionProperties, debug);
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
    public long createFlow(String studyName, List<Task> tasks) throws PersistenceException {
        return 0; // TODO call dbProxy
    }

    @Override
    public void startFlow(long flowId) {
        // TODO call dbProxy - update tasks linked to flow - turn to status NEW (status on insert would be CREATED)
    }

    @Override
    public void cancelFlow(long flowId) {
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
    public List<Task> getTasks(long flowId) throws PersistenceException {
        return null; // TODO call dbProxy
    }

    @Override
    public void cancelTask(Task task) throws PersistenceException {
        // TODO call dbProxy
    }

    @Override
    public Unit getUnit(long unitId) throws PersistenceException {
        return unitFetcher.getUnit(unitId);
    }

    @Override
    public List<Unit> getAllUnits() throws PersistenceException {
        return unitFetcher.getAllUnits();
    }

}
