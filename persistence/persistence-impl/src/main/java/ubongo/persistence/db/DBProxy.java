package ubongo.persistence.db;

import com.google.gson.JsonParseException;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sun.istack.internal.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.datatypes.*;
import ubongo.persistence.exceptions.UnitFetcherException;
import ubongo.common.networkUtils.SSHConnection;
import ubongo.common.networkUtils.SSHConnectionProperties;
import ubongo.persistence.exceptions.DBProxyException;
import ubongo.persistence.UnitFetcher;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages the communication with the DB to separate
 * queue management logic (QueueManager) and DB implementation details.
 */
public class DBProxy {

    // TODO add retry mechanism to all methods that send statements to the DB

    private static Logger logger = LogManager.getLogger(DBProxy.class);

    private Session sshSession;
    private SSHConnectionProperties sshProperties;
    private boolean useSSH;
    private Connection connection;
    private DBConnectionProperties dbProperties;
    private int localPort;
    private UnitFetcher unitFetcher;

    private boolean debug = false;

    public DBProxy(UnitFetcher unitFetcher, DBConnectionProperties dbConnectionProperties) {
        this(unitFetcher, dbConnectionProperties, false);
    }

    public DBProxy(UnitFetcher unitFetcher, DBConnectionProperties dbConnectionProperties, boolean debug) {
        dbProperties = dbConnectionProperties;
        this.unitFetcher = unitFetcher;
        useSSH = false;
        this.debug = debug;
    }

    public DBProxy(UnitFetcher unitFetcher, DBConnectionProperties dbConnectionProperties,
                   SSHConnectionProperties sshConnectionProperties) {
        this(unitFetcher, dbConnectionProperties);
        sshProperties = sshConnectionProperties;
        useSSH = true;
    }

    // For debugging and tests
    public DBProxy(UnitFetcher unitFetcher, DBConnectionProperties dbConnectionProperties,
                   SSHConnectionProperties sshConnectionProperties,
                   boolean debug) {
        this(unitFetcher, dbConnectionProperties, sshConnectionProperties);
        this.debug = debug;
    }

    public void start() throws DBProxyException {
        connect();
    }

    public void connect() throws DBProxyException {
        try {
            if (useSSH && (sshSession == null || !sshSession.isConnected())) {
                try {
                    sshSession = SSHConnection.establish(sshProperties);
                    localPort = getFreeLocalPort();
                    logger.info("Setting SSH Tunneling to remote DB (" + dbProperties.getHost() + ":" + dbProperties.getPort()
                            + ") using local port " + localPort + "...");
                    sshSession.setPortForwardingL(localPort, dbProperties.getHost(), dbProperties.getPort());
                } catch (JSchException e) {
                    String errorMsg = "Failed to establish SSH connection to the database";
                    logger.error(errorMsg);
                    throw new DBProxyException(errorMsg);
                }
            }
            if (connection == null || connection.isClosed()) {
                logger.info("Establishing connection to " + getUrl() + " with user " + getUser() + "...");
                String driver = "com.mysql.jdbc.Driver";
                try {
                    Class.forName(driver);
                    connection = DriverManager.getConnection(getActualUrl(), getUser(), dbProperties.getPassword());
                } catch (SQLException e) {
                    String errorMsg = String.format("Failed to connect to the database (url: %s; user: %s)", getUrl(), getUser());
                    logSqlException(e, errorMsg);
                    throw new DBProxyException(errorMsg);
                } catch (ClassNotFoundException e) {
                    throw new DBProxyException("Database connection cannot be established. MySQL JDBC driver class (" + driver + ") was not found");
                }
                logger.info("Connected to DB at " + getUrl());
            }
        } catch (SQLException e) {
            String errorMsg =
                    String.format("Failed to connect to the database (url: %s; user: %s).", getUrl(), getUser());
            logSqlException(e, errorMsg);
            throw new DBProxyException(errorMsg);
        }
    }

    public void disconnect() throws DBProxyException {
        try {
            boolean alreadyClosed = true;
            if (connection != null && !connection.isClosed()) {
                alreadyClosed = false;
                logger.info("Closing connection to " + getUrl() + "...");
                connection.close();
            }
            if (sshSession != null && sshSession.isConnected()) {
                alreadyClosed = false;
                logger.info("Closing SSH Connection to host: " + sshSession.getHost() + ":" + sshSession.getPort() + "...");
                sshSession.disconnect();
            }
            if (!alreadyClosed) {
                logger.info("Successfully closed database connection via SSH tunneling");
            }
        } catch (SQLException e) {
            String errorMsg =
                    String.format("Failed to disconnect from the database (url: %s; user: %s).", getUrl(), getUser());
            logSqlException(e, errorMsg);
            throw new DBProxyException(errorMsg);
        }
    }

    /**
     * updates the given task's status in the DB (based on id)
     * A task may not change from Processing to Pending (it must be cancelled or completed beforehand)
     * @param task to updateTaskStatus in DB.
     *             If the task ID cannot be found in the DB, this method does nothing
     */
    public void updateStatus(Task task) throws DBProxyException {
        connect();
        String tableName = getTableName(DBConstants.TASKS_TABLE_NAME);
        try {
            String sql = Queries.getQuery("update_task_status")
                    .replace("$tasksTable", tableName);
            PreparedStatement statement = connection.prepareStatement(sql);
            String status = getStatusString(task.getStatus());
            // if the current status is Processing, don't update to Pending -
            // this situation may be caused by threads synchronization issues and is not desired
            // status to set if the current status is Processing
            if (task.getStatus() == TaskStatus.PENDING) {
                statement.setString(1, getStatusString(TaskStatus.PROCESSING));
            } else {
                statement.setString(1, status);
            }
            statement.setString(2, status); // status to set if the current status is not Processing
            // update execution time and machine if relevant
            if (task.getStatus() == TaskStatus.PROCESSING) {
                statement.setInt(3, task.getMachine().getId());
            } else {
                statement.setNull(3, Types.INTEGER);
            }
            statement.setInt(4, task.getId()); // id of task to update
            int numRowsUpdated = executeUpdate(statement);
            if (numRowsUpdated != 1) {
                // TODO handle update failure
            }
        } catch (SQLException e) {
            String errorMsg = "Failed to update task's status in DB (taskId="
                    + task.getId() + ", newStatus=" + task.getStatus() + ")";
            logSqlException(e, errorMsg);
            throw new DBProxyException(errorMsg);
        }
    }

    /**
     * @throws DBProxyException if query fails or if there is no task with the given id in the DB
     */
    // TODO
    public Task getTask(int id) throws DBProxyException {
        connect();
        Task task;
        String tableName = getTableName(DBConstants.TASKS_TABLE_NAME);
        try {
            String sql = Queries.getQuery("get_task_by_id").replace("$tasksTable", tableName);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            ResultSet resultSet = executeQuery(statement);
            if (!resultSet.next()) {
                throw new DBProxyException("No task with id=" + id + "exists in DB");
            }
            task = new Task();
            task.setStatus(TaskStatus.valueOf(resultSet.getString(DBConstants.TASKS_TASK_STATUS).toUpperCase()));
            task.setId(resultSet.getInt(DBConstants.TASKS_TASK_ID));
            task.setMachine(null); // TODO
            Unit unit = unitFetcher.getUnit(resultSet.getInt(DBConstants.TASKS_UNIT_ID));
            unit.setParameterValues(resultSet.getString(DBConstants.TASKS_UNIT_PARAMS));
            task.setUnit(unit);
        } catch (SQLException e) {
            String errorMsg = "Failed to get task from DB by id (" + id + ")";
            logSqlException(e, errorMsg);
            throw new DBProxyException(errorMsg);
        } catch (UnitFetcherException e) {
            // TODO DispatcherImpl.notifyFatal(e); // TODO what now?
            return null;
        }
        return task;
    }

    // TODO
    public List<Task> getNewTasks() throws DBProxyException, UnitFetcherException {
        connect();
        List<Task> tasks = new ArrayList<>();
        String tableName = getTableName(DBConstants.TASKS_TABLE_NAME);
        try {
            String sql = Queries.getQuery("get_new_tasks").replace("$tasksTable", tableName);
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = executeQuery(statement);
            while (resultSet.next()) {
                Task task = new Task();
                task.setStatus(TaskStatus.NEW);
                task.setId(resultSet.getInt(DBConstants.TASKS_TASK_ID));
                Unit unit = unitFetcher.getUnit(resultSet.getInt(DBConstants.TASKS_UNIT_ID));
                unit.setParameterValues(resultSet.getString(DBConstants.TASKS_UNIT_PARAMS));
                task.setUnit(unit);
                task.setId(resultSet.getInt(DBConstants.TASKS_TASK_ID));
                tasks.add(task);
            }
        } catch (SQLException e) {
            String errorMsg = "Failed to retrieve new tasks from DB";
            logSqlException(e, errorMsg);
            throw new DBProxyException(errorMsg);
        } catch (JsonParseException e) {
            // TODO DispatcherImpl.notifyFatal(e); // TODO what now?
            return new ArrayList<>(); // return empty task list
        }
        return tasks;
    }

    public void createAnalysis(String analysisName, List<Unit> units) {
        // TODO
    }

    public int createFlow(String studyName, List<Task> tasks) throws DBProxyException {
        connect();
        String tasksTableName = getTableName(DBConstants.TASKS_TABLE_NAME);
        String flowsTableName = getTableName(DBConstants.FLOWS_TABLE_NAME);
        for (Task task : tasks) {
            task.setStatus(TaskStatus.CREATED);
        }
        try {
            String values = getTasksAsValueList(tasks);
            if (values == null) {
                String errMsg = "System tried to add an empty list of tasks to the database";
                logger.warn(errMsg);
                throw new DBProxyException(errMsg);
            }
            String sql = Queries.getQuery("create_flow")
                    .replace("$flowsTable", flowsTableName)
                    .replace("$tasksTable", tasksTableName)
                    .replace("$values", values);
            PreparedStatement statement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, studyName);
            int numRowsAdded = executeUpdate(statement);
            if (numRowsAdded != tasks.size() + 1) {
                // TODO what happens if not all rows were created?
                System.out.println("Updated " + numRowsAdded + " rows. Expected: " + (tasks.size() + 1));
            }
            ResultSet results = statement.getGeneratedKeys();
            results.next();
            int flowId = results.getInt(1); // TODO test it returns the flowId
            return flowId;
        } catch (SQLException e) {
            String errorMsg = "Failed to add tasks to DB";
            logSqlException(e, errorMsg);
            throw new DBProxyException(errorMsg);
        }
    }

    public void startFlow(int flowId) {
        // TODO

    }

    public void cancelFlow(int flowId) {
        // TODO
    }

    public List<Task> getTasks(int flowId) {
        // TODO
        return null;
    }

    public void cancelTask(Task task) {
        // TODO
    }


















    private String getTasksAsValueList(List<Task> tasks) {
        // (status, flow_id, serial_in_flow, unit_id, unit_params, machine_id)
        List<String> valuesList = new ArrayList<>();
        for (Task task : tasks) {
            Unit unit = task.getUnit();
            Machine machine = task.getMachine();
            // we insert a flow to the DB before inserting tasks so LAST_INSERT_ID() returns the flowId
            valuesList.add(concatStrings(
                    "('", getStatusString(task.getStatus()), "', LAST_INSERT_ID(), ",
                    Integer.toString(task.getSerialNumber()), ", ",
                    ((unit == null)?"NULL":Integer.toString(unit.getId())), ", ",
                    ((unit == null)?"NULL": "'" + getParametersJsonString(unit) + "'"), ", ",
                    ((machine == null)?"NULL": "'" + machine.getId() + "'"), ")"
            ));
        }
        return StringUtils.join(valuesList, ',');
    }

    private String concatStrings(String ... strings) {
        StringBuilder sb = new StringBuilder();
        for (String string : strings) {
            sb.append(string);
        }
        return sb.toString();
    }

    private String getStatusString(TaskStatus status) {
        return StringUtils.capitalize(status.toString().toLowerCase());
    }

    @NotNull
    private String getParametersJsonString(Unit unit) {
        List<UnitParameter> params = unit.getParameters();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        boolean isFirst = true;
        for (UnitParameter param: params) {
            String nameValuePair = ((isFirst)?"":", ") + "\"" + param.getName() + "\": \"" + param.getValue() + "\"";
            stringBuilder.append(nameValuePair);
            isFirst = false;
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private String getUrl() {
        return dbProperties.getHost() + ":" + dbProperties.getPort() + "/" + dbProperties.getSchema();
    }

    private String getActualUrl() {
        String url;
        if (useSSH) {
            url = "jdbc:mysql://localhost:" + localPort + "/" + dbProperties.getSchema();
        } else {
            url = "jdbc:mysql://" + dbProperties.getHost() + ":" + dbProperties.getPort() + "/" + dbProperties.getSchema();
        }
        return url;
    }

    private int getFreeLocalPort() {
        logger.debug("Searching for available local port for SSH tunneling...");
        ServerSocket serverSocket = null;
        int portNumber;
        try {
            serverSocket = new ServerSocket(0); // get any available port
            portNumber = serverSocket.getLocalPort();
        } catch (IOException e) {
            portNumber = 49999; // fallback port (this line will probably never be executed)
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                // do nothing
            }
        }
        return portNumber;
    }

    private void logSqlException(SQLException e, String errorMsg) {
        logger.error(errorMsg +
                "\nSQLException: " + e.getMessage() +
                "\nSQLState: " + e.getSQLState() +
                "\nVendorError: " + e.getErrorCode());
    }

    private ResultSet executeQuery(PreparedStatement statement) throws SQLException {
        logger.debug("Executing: " + statement.toString());
        return statement.executeQuery();
    }

    private int executeUpdate(PreparedStatement statement) throws SQLException {
        logger.debug("Executing: " + statement.toString());
        return statement.executeUpdate();
    }

    private String getUser() {
        return dbProperties.getUser();
    }

    private String getTableName(String baseTableName) {
        return (!debug)?"":DBConstants.DEBUG_PREFIX + baseTableName;
    }

}
