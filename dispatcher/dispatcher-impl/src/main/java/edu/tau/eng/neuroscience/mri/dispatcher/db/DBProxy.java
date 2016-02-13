package edu.tau.eng.neuroscience.mri.dispatcher.db;

import com.google.gson.JsonParseException;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sun.xml.internal.ws.util.StringUtils;
import edu.tau.eng.neuroscience.mri.common.datatypes.*;
import edu.tau.eng.neuroscience.mri.common.exceptions.ErrorCodes;
import edu.tau.eng.neuroscience.mri.common.exceptions.UnitFetcherException;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.common.networkUtils.SSHConnection;
import edu.tau.eng.neuroscience.mri.common.networkUtils.SSHConnectionProperties;
import edu.tau.eng.neuroscience.mri.dispatcher.DispatcherImpl;
import edu.tau.eng.neuroscience.mri.dispatcher.UnitFetcher;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
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

    private static Logger logger = LoggerManager.getLogger(DBProxy.class);

    private Session sshSession;
    private SSHConnectionProperties sshProperties;
    private boolean useSSH;
    private Connection connection;
    private DBConnectionProperties dbProperties;
    private int localPort;

    private boolean debug = false;

    public DBProxy(String configurationFilePath) throws DBProxyException {
        dbProperties = loadDbConfig(configurationFilePath);
        useSSH = false;
    }

    public DBProxy(String dbConfigurationFilePath, String sshConfigurationFilePath) throws DBProxyException {
        dbProperties = loadDbConfig(dbConfigurationFilePath);
        sshProperties = loadSshConfig(sshConfigurationFilePath);
        useSSH = true;
    }

    /**
     * For debugging and tests
     */
    public DBProxy(String dbConfigurationFilePath, String sshConfigurationFilePath, boolean debug) throws DBProxyException {
        this.debug = debug;
        dbProperties = loadDbConfig(dbConfigurationFilePath);
        sshProperties = loadSshConfig(sshConfigurationFilePath);
        useSSH = true;
    }

    public String getUser() {
        return dbProperties.getUser();
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
                    throw new DBProxyException(ErrorCodes.SSH_CONNECTION_EXCEPTION, errorMsg);
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
                    throw new DBProxyException(ErrorCodes.DB_CONNECTION_EXCEPTION, errorMsg);
                } catch (ClassNotFoundException e) {
                    throw new DBProxyException(ErrorCodes.GENERAL_DB_PROXY_EXCEPTION,
                            "Database connection cannot be established. MySQL JDBC driver class (" + driver + ") was not found");
                }
                logger.info("Connected to DB at " + getUrl());
            }
        } catch (SQLException e) {
            String errorMsg =
                    String.format("Failed to connect to the database (url: %s; user: %s).", getUrl(), getUser());
            logSqlException(e, errorMsg);
            throw new DBProxyException(ErrorCodes.DB_CONNECTION_EXCEPTION, errorMsg);
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
            throw new DBProxyException(ErrorCodes.DB_CONNECTION_EXCEPTION, errorMsg);
        }
    }

    // TODO check if a similar task is already in the DB before inserting a duplicate
    public void add(List<Task> tasks) throws DBProxyException {
        connect();
        String tableName = ((!debug)?"":DBConstants.DEBUG_PREFIX) + DBConstants.TASKS_TABLE_NAME;
        try {
            String values = getTasksAsValueList(tasks);
            if (values == null) {
                logger.warn("System tried to add an empty list of tasks to the database");
                return;
            }
            String sql = Queries.getQuery("add_tasks")
                    .replace("$tableName", tableName)
                    .replace("$values", values);
            PreparedStatement statement = connection.prepareStatement(sql);
            int numRowsAdded = executeUpdate(statement);
            if (numRowsAdded != tasks.size()) {
                // TODO what happens if not all rows were created?
                System.out.println("Updated " + numRowsAdded + " rows. Expected: " + tasks.size());
            }
        } catch (SQLException e) {
            String errorMsg = "Failed to add tasks to DB";
            logSqlException(e, errorMsg);
            throw new DBProxyException(ErrorCodes.QUERY_FAILURE_EXCEPTION, errorMsg);
        }
    }

    private String getTasksAsValueList(List<Task> tasks) {
        //(status, unit_id, unit_params, machine_id)
        String values = tasks.stream()
                .map((task) -> {
                    Unit unit = task.getUnit();
                    Machine machine = task.getMachine();
                    return "('" + getStatusString(task.getStatus()) + "'," + ((unit == null)?"NULL":unit.getId()) +
                            "," + ((unit == null)?"NULL": "'" + getParametersJsonString(unit) + "'") +
                            "," + ((machine == null)?"NULL": "'" + machine.getId() + "'") + "),";
                }).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
        if (values.length() == 0) {
            values = null;
        } else if (values.charAt(values.length()-1) == ',') {
            values = values.substring(0, values.length()-1);
        }
        return values;
    }

    private String getStatusString(TaskStatus status) {
        return StringUtils.capitalize(status.toString().toLowerCase());
    }

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

    public void add(Task task) throws DBProxyException {
        List<Task> tasks = new ArrayList<>();
        tasks.add(task);
        this.add(tasks);
    }

    /**
     * updates the given task's status in the DB (based on id)
     * A task may not change from Processing to Pending (it must be cancelled or completed beforehand)
     * @param task to updateStatus in DB.
     *             If the task ID cannot be found in the DB, this method does nothing
     */
    public void updateStatus(Task task) throws DBProxyException {
        connect();
        String tableName = ((!debug)?"":DBConstants.DEBUG_PREFIX) + DBConstants.TASKS_TABLE_NAME;
        try {
            String sql = Queries.getQuery("update_task_status")
                    .replace("$tableName", tableName);
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
            // update execution time if relevant
            if (task.getStatus() == TaskStatus.PROCESSING) {
                statement.setTimestamp(3, getCurrentTimeStamp());
                statement.setInt(5, task.getMachine().getId());
            } else {
                statement.setNull(3, Types.TIMESTAMP);
                statement.setNull(5, Types.INTEGER);
            }
            // update completion time if relevant
            if (task.getStatus() == TaskStatus.COMPLETED) {
                statement.setTimestamp(4, getCurrentTimeStamp());
            } else {
                statement.setNull(4, Types.TIMESTAMP);
            }
            statement.setInt(6, task.getId()); // id of task to update
            int numRowsUpdated = executeUpdate(statement);
            if (numRowsUpdated != 1) {
                // TODO handle update failure
            }
        } catch (SQLException e) {
            String errorMsg = "Failed to update task's status in DB (taskId="
                    + task.getId() + ", newStatus=" + task.getStatus() + ")";
            logSqlException(e, errorMsg);
            throw new DBProxyException(ErrorCodes.QUERY_FAILURE_EXCEPTION, errorMsg);
        }
    }

    private Timestamp getCurrentTimeStamp() {
        return new Timestamp((new java.util.Date()).getTime());
    }

    /**
     * @throws DBProxyException if query fails or if there is no task with the given id in the DB
     */
    public Task getTask(int id) throws DBProxyException {
        connect();
        Task task;
        String tableName = (!debug)?"":DBConstants.DEBUG_PREFIX + DBConstants.TASKS_TABLE_NAME;
        try {
            String sql = Queries.getQuery("get_task_by_id").replace("$tableName", tableName);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            ResultSet resultSet = executeQuery(statement);
            if (!resultSet.next()) {
                throw new DBProxyException(ErrorCodes.EMPTY_RESULT_SET, "No task with id=" + id + "exists in DB");
            }
            task = new TaskImpl();
            task.setStatus(TaskStatus.valueOf(resultSet.getString(DBConstants.TASKS_TASK_STATUS).toUpperCase()));
            task.setId(resultSet.getInt(DBConstants.TASKS_TASK_ID));
            task.setMachine(null); // TODO
            Unit unit = UnitFetcher.getUnit(resultSet.getInt(DBConstants.TASKS_UNIT_ID));
            unit.setParameterValues(resultSet.getString(DBConstants.TASKS_UNIT_PARAMS));
            task.setUnit(unit);
        } catch (SQLException e) {
            String errorMsg = "Failed to get task from DB by id (" + id + ")";
            logSqlException(e, errorMsg);
            throw new DBProxyException(ErrorCodes.QUERY_FAILURE_EXCEPTION, errorMsg);
        } catch (UnitFetcherException e) {
            DispatcherImpl.notifyFatal(e); // TODO what now?
            return null;
        }
        return task;
    }

    public List<Task> getNewTasks() throws DBProxyException, UnitFetcherException {
        connect();
        List<Task> tasks = new ArrayList<>();
        String tableName = (!debug)?"":DBConstants.DEBUG_PREFIX + DBConstants.TASKS_TABLE_NAME;
        try {
            String sql = Queries.getQuery("get_new_tasks").replace("$tableName", tableName);
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = executeQuery(statement);
            while (resultSet.next()) {
                Task task = new TaskImpl();
                task.setStatus(TaskStatus.NEW);
                task.setId(resultSet.getInt(DBConstants.TASKS_TASK_ID));
                Unit unit = UnitFetcher.getUnit(resultSet.getInt(DBConstants.TASKS_UNIT_ID));
                unit.setParameterValues(resultSet.getString(DBConstants.TASKS_UNIT_PARAMS));
                task.setUnit(unit);
                task.setId(resultSet.getInt(DBConstants.TASKS_TASK_ID));
                tasks.add(task);
            }
        } catch (SQLException e) {
            String errorMsg = "Failed to retrieve new tasks from DB";
            logSqlException(e, errorMsg);
            throw new DBProxyException(ErrorCodes.QUERY_FAILURE_EXCEPTION, errorMsg);
        } catch (JsonParseException e) {
            DispatcherImpl.notifyFatal(e); // TODO what now?
            return new ArrayList<>(); // return empty task list
        }
        return tasks;
    }

    public String getUrl() {
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

    private DBConnectionProperties loadDbConfig(String configFilePath) throws DBProxyException {
        DBConnectionProperties dbConnectionProperties = null;
        File file = new File(configFilePath);
        logger.debug("Loading DB Connection configuration details from " + file.getAbsolutePath() + "...");
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(DBConnectionProperties.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            dbConnectionProperties = (DBConnectionProperties) unmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            String originalMsg = e.getMessage();
            String msg = "Failed to parse Database Connection configuration file (file path: "
                    + file.getAbsolutePath() + "). " + ((originalMsg == null) ? "" : "Details: " + originalMsg);
            logger.error(msg);
        }
        if (dbConnectionProperties == null) {
            throw new DBProxyException(ErrorCodes.DB_CONNECTION_PROPERTIES_UNMARSHAL_EXCEPTION,
                    "Failed to retrieve Database Connection configuration. " +
                            "Make sure that " + file.getAbsolutePath() + " exists and is configured correctly");
        }
        return dbConnectionProperties;
    }

    private SSHConnectionProperties loadSshConfig(String configFilePath) throws DBProxyException {
        SSHConnectionProperties sshConnectionProperties = null;
        File file = new File(configFilePath);
        logger.debug("Loading SSH Connection configuration details from " + file.getAbsolutePath() + "...");
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(SSHConnectionProperties.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            sshConnectionProperties = (SSHConnectionProperties) unmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            String originalMsg = e.getMessage();
            String msg = "Failed to parse SSH Connection configuration file (file path: "
                    + file.getAbsolutePath() + "). " + ((originalMsg == null) ? "" : "Details: " + originalMsg);
            logger.error(msg);
        }
        if (sshConnectionProperties == null) {
            throw new DBProxyException(ErrorCodes.SSH_CONNECTION_PROPERTIES_UNMARSHAL_EXCEPTION,
                    "Failed to retrieve SSH Connection configuration. " +
                            "Make sure that " + file.getAbsolutePath() + " exists and is configured correctly");
        }
        return sshConnectionProperties;
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

}
