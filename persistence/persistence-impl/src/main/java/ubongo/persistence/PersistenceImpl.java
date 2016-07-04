package ubongo.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.datatypes.FlowData;
import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;
import ubongo.common.networkUtils.SSHConnectionProperties;
import ubongo.persistence.db.DBConnectionProperties;
import ubongo.persistence.db.DBProxy;
import ubongo.persistence.db.SQLExceptionHandler;
import ubongo.persistence.exceptions.DBProxyException;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

public class PersistenceImpl implements Persistence {

    /**
     * In some cases, failing queries or updates to the DB may produce successful results
     * given another chance (i.e. SQLTransientException).
     * MAX_NUM_RETRIES defines the number of retries in case of such errors.
     */
    private static final int MAX_NUM_RETRIES = 3;
    private static final Logger logger = LogManager.getLogger(PersistenceImpl.class);

    private DBProxy dbProxy;
    private SQLExceptionHandler sqlExceptionHandler;
    private UnitFetcher unitFetcher;

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
        dbProxy = sshConnectionProperties != null ?
                new DBProxy(unitFetcher, dbConnectionProperties, sshConnectionProperties, machines, debug) :
                new DBProxy(unitFetcher, dbConnectionProperties, machines, debug);
        sqlExceptionHandler = new SQLExceptionHandler(dbProxy);
    }

    @Override
    public void start() throws PersistenceException {
        new DBMethodInvoker<>(sqlExceptionHandler, dbProxy::start).invoke();
    }

    @Override
    public void stop() throws PersistenceException {
        dbProxy.disconnect();
    }

    @Override
    public void createAnalysis(String analysisName, List<Unit> units) throws PersistenceException {
        int numRetries = 0;
        while (numRetries++ < MAX_NUM_RETRIES) {
            try {
                dbProxy.createAnalysis(analysisName, units);
                return;
            } catch (DBProxyException e) {
                DBProxyException ret;
                if ((ret = handleDbProxyException(e, numRetries)) != null) throw ret;
            }
        }
        throw new PersistenceException("Unknown reason"); // not possible
    }

    @Override
    public List<String> getAnalysisNames() throws PersistenceException {
        return new DBMethodInvoker<>(sqlExceptionHandler, dbProxy::getAnalysisNames).invoke();
    }

    @Override
    public List<Unit> getAnalysis(String analysisName) throws PersistenceException {
        int numRetries = 0;
        while (numRetries++ < MAX_NUM_RETRIES) {
            try {
                return dbProxy.getAnalysis(analysisName);
            } catch (DBProxyException e) {
                DBProxyException ret;
                if ((ret = handleDbProxyException(e, numRetries)) != null) throw ret;
            }
        }
        throw new PersistenceException("Unknown reason"); // not possible
    }

    @Override
    public int createFlow(String studyName, List<Task> tasks) throws PersistenceException {
        int numRetries = 0;
        while (numRetries++ < MAX_NUM_RETRIES) {
            try {
                return dbProxy.createFlow(studyName, tasks);
            } catch (DBProxyException e) {
                DBProxyException ret;
                if ((ret = handleDbProxyException(e, numRetries)) != null) throw ret;
            }
        }
        throw new PersistenceException("Unknown reason"); // not possible
    }

    @Override
    public void startFlow(int flowId) throws PersistenceException {
        int numRetries = 0;
        while (numRetries++ < MAX_NUM_RETRIES) {
            try {
                dbProxy.startFlow(flowId);
                return;
            } catch (DBProxyException e) {
                DBProxyException ret;
                if ((ret = handleDbProxyException(e, numRetries)) != null) throw ret;
            }
        }
        throw new PersistenceException("Unknown reason"); // not possible
    }

    @Override
    public List<Task> cancelFlow(int flowId) throws PersistenceException {
        int numRetries = 0;
        while (numRetries++ < MAX_NUM_RETRIES) {
            try {
                return dbProxy.cancelFlow(flowId);
            } catch (DBProxyException e) {
                DBProxyException ret;
                if ((ret = handleDbProxyException(e, numRetries)) != null) throw ret;
            }
        }
        throw new PersistenceException("Unknown reason"); // not possible
    }

    @Override
    public List<Task> getNewTasks() throws PersistenceException {
        return new DBMethodInvoker<>(sqlExceptionHandler, dbProxy::getNewTasks).invoke();
    }

    @Override
    public void updateTaskStatus(Task task) throws PersistenceException {
        int numRetries = 0;
        while (numRetries++ < MAX_NUM_RETRIES) {
            try {
                dbProxy.updateStatus(task);
                return;
            } catch (DBProxyException e) {
                DBProxyException ret;
                if ((ret = handleDbProxyException(e, numRetries)) != null) throw ret;
            }
        }
        throw new PersistenceException("Unknown reason"); // not possible
    }

    @Override
    public void updateTasksStatus(Collection<Task> waitingTasks) throws PersistenceException {
        int numRetries = 0;
        while (numRetries++ < MAX_NUM_RETRIES) {
            try {
                dbProxy.updateStatus(waitingTasks);
                return;
            } catch (DBProxyException e) {
                DBProxyException ret;
                if ((ret = handleDbProxyException(e, numRetries)) != null) throw ret;
            }
        }
        throw new PersistenceException("Unknown reason"); // not possible
    }

    @Override
    public List<Task> getTasks(int flowId) throws PersistenceException {
        int numRetries = 0;
        while (numRetries++ < MAX_NUM_RETRIES) {
            try {
                return dbProxy.getTasks(flowId);
            } catch (DBProxyException e) {
                DBProxyException ret;
                if ((ret = handleDbProxyException(e, numRetries)) != null) throw ret;
            }
        }
        throw new PersistenceException("Unknown reason"); // not possible
    }

    @Override
    public boolean cancelTask(Task task) throws PersistenceException {
        int numRetries = 0;
        while (numRetries++ < MAX_NUM_RETRIES) {
            try {
                return dbProxy.cancelTask(task);
            } catch (DBProxyException e) {
                DBProxyException ret;
                if ((ret = handleDbProxyException(e, numRetries)) != null) throw ret;
            }
        }
        throw new PersistenceException("Unknown reason"); // not possible
    }

    @Override
    public Unit getUnit(int unitId) throws PersistenceException {
        return unitFetcher.getUnit(unitId);
    }

    @Override
    public List<Unit> getAllUnits() throws PersistenceException {
        return new DBMethodInvoker<>(sqlExceptionHandler, unitFetcher::getAllUnits).invoke();
    }

    @Override
    public List<Task> getAllTasks(int limit) {
        return null; // TODO
    }

    @Override
    public List<FlowData> getAllFlows(int limit) {
        return null; // TODO
    }

    @Override
    public void resumeTask(Task task) {
        // TODO
    }

    public void clearDebugData() throws PersistenceException {
        new DBMethodInvoker<>(sqlExceptionHandler, dbProxy::clearAllDebugTables).invoke();
    }

    private DBProxyException handleDbProxyException(DBProxyException e, int numRetries) {
        Throwable t = e.getCause();
        if (numRetries == MAX_NUM_RETRIES || !(t instanceof SQLException)
                || !sqlExceptionHandler.isRecoverable((SQLException) t)) {
            logger.error(e.getMessage(), e);
            return e;
        }
        return null;
    }

    private class DBMethodInvoker<T> {

        private Callable<T> callable;
        private SQLExceptionHandler sqlExceptionHandler;

        public DBMethodInvoker(SQLExceptionHandler sqlExceptionHandler, Callable<T> func) {
            this.callable = func;
            this.sqlExceptionHandler = sqlExceptionHandler;
        }

        public T invoke() throws DBProxyException {
            int numRetries = 0;
            String errMsg = "Failed to invoke method";
            DBProxyException dbProxyException = new DBProxyException(errMsg);
            while (numRetries++ < MAX_NUM_RETRIES) {
                try {
                    return callable.call();
                } catch (DBProxyException e) {
                    dbProxyException = e;
                    Throwable t = e.getCause();
                    if (!(t instanceof SQLException) || !sqlExceptionHandler.isRecoverable((SQLException) t)) {
                        throw e;
                    }
                } catch (Exception e) {
                    throw new DBProxyException(errMsg, e);
                }
            }
            throw dbProxyException;
        }
    }

}
