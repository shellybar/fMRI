package ubongo.persistence;

import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;
import ubongo.persistence.db.DBProxy;

import java.nio.file.Path;
import java.util.List;

public class PersistenceImpl implements Persistence {

    DBProxy dbProxy;
    UnitFetcher unitFetcher;

    PersistenceImpl(String unitSettingsDirPath, String dbConfigurationFilePath,
                    String sshConfigurationFilePath) throws PersistenceException {
        this(unitSettingsDirPath, dbConfigurationFilePath, sshConfigurationFilePath, false);
    }

    PersistenceImpl(String unitSettingsDirPath, String dbConfigurationFilePath) throws PersistenceException {
        this(unitSettingsDirPath, dbConfigurationFilePath, null, false);
    }

    PersistenceImpl(String unitSettingsDirPath, String dbConfigurationFilePath,
                    String sshConfigurationFilePath, boolean debug) throws PersistenceException {
        unitFetcher = new UnitFetcher(unitSettingsDirPath);

        // Database
        if (sshConfigurationFilePath != null) {
            dbProxy = new DBProxy(unitFetcher, dbConfigurationFilePath, sshConfigurationFilePath, debug);
        } else {
            dbProxy = new DBProxy(unitFetcher, dbConfigurationFilePath, debug);
        }
    }

    public void start() throws PersistenceException {
        dbProxy.start();
    }

    public void stop() throws PersistenceException {
        dbProxy.disconnect();
    }

    @Override
    public long createFlow(String studyName, Path studyRootDir) throws PersistenceException {
        return 0;
    }

    @Override
    public void cancelFlow(long flowId) {

    }

    @Override
    public void addTasks(List<Task> tasks) throws PersistenceException {

    }

    @Override
    public List<Task> getNewTasks() throws PersistenceException {
        return null;
    }

    @Override
    public void updateTaskStatus(Task task) throws PersistenceException {

    }

    @Override
    public List<Task> getTasks(long flowId) throws PersistenceException {
        return null;
    }

    @Override
    public void cancelTask(Task task) throws PersistenceException {

    }

    @Override
    public Unit getUnit(long unitId) throws PersistenceException {
        return null;
    }

    @Override
    public List<Unit> getAllUnits() throws PersistenceException {
        return null;
    }

}
