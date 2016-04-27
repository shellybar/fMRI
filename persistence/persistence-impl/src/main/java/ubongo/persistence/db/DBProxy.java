package ubongo.persistence.db;

import com.google.gson.JsonParseException;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sun.istack.internal.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.Utils;
import ubongo.common.datatypes.*;
import ubongo.persistence.exceptions.UnitFetcherException;
import ubongo.common.networkUtils.SSHConnection;
import ubongo.common.networkUtils.SSHConnectionProperties;
import ubongo.persistence.exceptions.DBProxyException;
import ubongo.persistence.UnitFetcher;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DBProxy {

    // TODO add retry mechanism to all methods that send statements to the DB

    private static Logger logger = LogManager.getLogger(DBProxy.class);

    private Session sshSession;
    private SSHConnectionProperties sshProperties;
    private boolean useSSH;
    private Map<Integer, Machine> machines;
    private Connection connection;
    private DBConnectionProperties dbProperties;
    private int localPort;
    private UnitFetcher unitFetcher;

    private boolean debug = false;

    public DBProxy(UnitFetcher unitFetcher,
                   DBConnectionProperties dbConnectionProperties, List<Machine> machines) {
        this(unitFetcher, dbConnectionProperties, machines, false);
    }

    public DBProxy(UnitFetcher unitFetcher, DBConnectionProperties dbConnectionProperties,
                   List<Machine> machines, boolean debug) {
        this.dbProperties = dbConnectionProperties;
        this.unitFetcher = unitFetcher;
        this.machines = machines == null ? new HashMap<>() : machines.stream()
                .collect(Collectors.toMap(Machine::getId, Function.identity()));
        this.useSSH = false;
        this.debug = debug;
    }

    public DBProxy(UnitFetcher unitFetcher, DBConnectionProperties dbConnectionProperties,
                   SSHConnectionProperties sshConnectionProperties, List<Machine> machines) {
        this(unitFetcher, dbConnectionProperties, machines);
        this.sshProperties = sshConnectionProperties;
        this.useSSH = true;
    }

    // For debugging and tests
    public DBProxy(UnitFetcher unitFetcher, DBConnectionProperties dbConnectionProperties,
                   SSHConnectionProperties sshConnectionProperties,
                   List<Machine> machines, boolean debug) {
        this(unitFetcher, dbConnectionProperties, sshConnectionProperties, machines);
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
                    throw new DBProxyException(errorMsg, e);
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
                    throw new DBProxyException(errorMsg, e);
                } catch (ClassNotFoundException e) {
                    throw new DBProxyException("Database connection cannot be established. " +
                            "MySQL JDBC driver class (" + driver + ") was not found", e);
                }
                logger.info("Connected to DB at " + getUrl());
            }
        } catch (SQLException e) {
            String errorMsg =
                    String.format("Failed to connect to the database (url: %s; user: %s).", getUrl(), getUser());
            logSqlException(e, errorMsg);
            throw new DBProxyException(errorMsg, e);
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
            throw new DBProxyException(errorMsg, e);
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
            String sql = Queries.getQuery(DBConstants.QUERY_UPDATE_TASK_STATUS)
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
            throw new DBProxyException(errorMsg, e);
        }
    }

    public void updateStatus(Collection<Task> tasks) throws DBProxyException {
        //TODO do it in one go
        for (Task task : tasks) {
            updateStatus(task);
        }

    }

    public void createAnalysis(String analysisName, List<Unit> units) {
        // TODO
    }

    public List<Unit> getAnalysis(String analysisName) {
        // TODO add to API
        return null;
    }

    public List<String> getAnalysisNames() {
        // TODO add to API
        return null;
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
            String sql = Queries.getQuery(DBConstants.QUERY_CREATE_FLOW)
                    .replace("$flowsTable", flowsTableName)
                    .replace("$tasksTable", tasksTableName)
                    .replace("$values", values);
            PreparedStatement statement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, studyName);
            executeUpdate(statement);
            ResultSet results = statement.getGeneratedKeys();
            results.next();
            return results.getInt(1);
        } catch (SQLException e) {
            String errorMsg = "Failed to add tasks to DB";
            logSqlException(e, errorMsg);
            throw new DBProxyException(errorMsg, e);
        }
    }

    public void startFlow(int flowId) throws DBProxyException {
        connect();
        String tasksTableName = getTableName(DBConstants.TASKS_TABLE_NAME);
        try {
            String sql = Queries.getQuery(DBConstants.QUERY_START_FLOW)
                    .replace("$tasksTable", tasksTableName);
            PreparedStatement statement =
                    connection.prepareStatement(sql);
            statement.setInt(1, flowId);
            executeUpdate(statement);
        } catch (SQLException e) {
            String errorMsg = "Failed to start flow in DB";
            logSqlException(e, errorMsg);
            throw new DBProxyException(errorMsg, e);
        }
    }

    public List<Task> cancelFlow(int flowId) {
        // TODO cancel tasks and return tasksToKill
        return null;
    }

    public boolean cancelTask(Task task) {
        // TODO if task is processing in DB then need to kill. In queue manager - if want to execute - make sure not canceled
        return false;
    }

    public Task getTask(int id) throws DBProxyException {
        List<Task> tasks = getTasks(DBConstants.QUERY_GET_TASK_BY_ID, id);
        if (tasks.size() == 0) {
            throw new DBProxyException("No task in the DB match the given id (" + id + ")");
        }
        return tasks.get(0);
    }

    public List<Task> getNewTasks() throws DBProxyException {
        return getTasks(DBConstants.QUERY_GET_NEW_TASKS);
    }

    public List<Task> getTasks(int flowId) throws DBProxyException {
        return getTasks(DBConstants.QUERY_GET_FLOW_TASKS, flowId);
    }

    public void clearAllDebugTables() throws DBProxyException {
        connect();
        String tasksTableName = getTableName(DBConstants.TASKS_TABLE_NAME, true);
        String flowsTableName = getTableName(DBConstants.FLOWS_TABLE_NAME, true);
        String unitsTableName = getTableName(DBConstants.UNITS_TABLE_NAME, true);
        try {
            String sql = Queries.getQuery(DBConstants.QUERY_CLEAR_TABLES)
                    .replace("$tasksTable", tasksTableName)
                    .replace("$flowsTable", flowsTableName)
                    .replace("$unitsTable", unitsTableName);
            PreparedStatement statement =
                    connection.prepareStatement(sql);
            executeUpdate(statement);
        } catch (SQLException e) {
            String errorMsg = "Failed to clear debug tables";
            logSqlException(e, errorMsg);
            throw new DBProxyException(errorMsg, e);
        }
    }

    private List<Task> getTasks(String queryName) throws DBProxyException {
        return getTasks(queryName, 0);
    }

    private List<Task> getTasks(String queryName, int id) throws DBProxyException {
        connect();
        List<Task> tasks = new ArrayList<>();
        String tableName = getTableName(DBConstants.TASKS_TABLE_NAME);
        String flowsTableName = getTableName(DBConstants.FLOWS_TABLE_NAME);
        String errorMsg = "Failed to retrieve tasks from DB";
        try {
            String sql = Queries.getQuery(queryName)
                    .replace("$flowsTable", flowsTableName)
                    .replace("$tasksTable", tableName);
            PreparedStatement statement = connection.prepareStatement(sql);
            if (queryName.equals(DBConstants.QUERY_GET_FLOW_TASKS) ||
                    queryName.equals(DBConstants.QUERY_GET_TASK_BY_ID)) {
                statement.setInt(1, id);
            }
            ResultSet resultSet = executeQuery(statement);
            while (resultSet.next()) {
                tasks.add(taskFromResultSet(resultSet));
            }
        } catch (SQLException e) {
            logSqlException(e, errorMsg);
            throw new DBProxyException(errorMsg, e);
        } catch (JsonParseException | UnitFetcherException e) {
            throw new DBProxyException(errorMsg, e);
        }
        return tasks;
    }

    private Task taskFromResultSet(ResultSet resultSet) throws SQLException, UnitFetcherException {
        Unit unit = unitFetcher.getUnit(resultSet.getInt(DBConstants.TASKS_UNIT_ID));
        unit.setParameterValues(resultSet.getString(DBConstants.TASKS_UNIT_PARAMS));
        int machineId = resultSet.getInt(DBConstants.TASKS_MACHINE_ID);
        Context context = new Context(
                resultSet.getString(DBConstants.TASKS_STUDY),
                resultSet.getString(DBConstants.TASKS_SUBJECT),
                resultSet.getString(DBConstants.TASKS_RUN)
        );

        return new Task(
                resultSet.getInt(DBConstants.TASKS_TASK_ID),
                resultSet.getInt(DBConstants.TASKS_FLOW_ID),
                resultSet.getInt(DBConstants.TASKS_SERIAL_NUM),
                unit,
                machineId == 0 ? null : machines.get(machineId),
                context,
                TaskStatus.valueOf(resultSet.getString(DBConstants.TASKS_TASK_STATUS).toUpperCase())
        );
    }

    private String getTasksAsValueList(List<Task> tasks) {
        // (status, flow_id, serial_in_flow, unit_id, unit_params, subject, run, machine_id)
        List<String> valuesList = new ArrayList<>();
        for (Task task : tasks) {
            Unit unit = task.getUnit();
            Machine machine = task.getMachine();

            // context
            String subject = "NULL";
            String run = "NULL";
            Context context = task.getContext();
            if (context != null) {
                if (context.getSubject() != null) subject = "'" + context.getSubject() + "'";
                if (context.getRun() != null) run = "'" + context.getRun() + "'";
            }

            // we insert a flow to the DB before inserting tasks so LAST_INSERT_ID() returns the flowId
            valuesList.add(Utils.concatStrings(
                    "('", getStatusString(task.getStatus()), "', LAST_INSERT_ID(), ",
                    Integer.toString(task.getSerialNumber()), ", ",
                    ((unit == null)?"NULL":Integer.toString(unit.getId())), ", ",
                    ((unit == null)?"NULL": "'" + getParametersJsonString(unit) + "'"), ", ",
                    subject, ", ", run, ", ",
                    ((machine == null)?"NULL": "'" + machine.getId() + "'"), ")"
            ));
        }
        return StringUtils.join(valuesList, ',');
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
        url += "?allowMultiQueries=true";
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
                "\n\tSQLException: " + e.getMessage() +
                "\n\tSQLState: " + e.getSQLState() +
                "\n\tVendorError: " + e.getErrorCode());
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

    private String getTableName(String baseTableName, boolean debug) {
        return ((!debug)?"":DBConstants.DEBUG_PREFIX) + baseTableName;
    }

    private String getTableName(String baseTableName) {
        return getTableName(baseTableName, debug);
    }
}
