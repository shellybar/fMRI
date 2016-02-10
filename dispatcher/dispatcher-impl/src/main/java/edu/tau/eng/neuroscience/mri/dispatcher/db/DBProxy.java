package edu.tau.eng.neuroscience.mri.dispatcher.db;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import edu.tau.eng.neuroscience.mri.common.datatypes.Task;
import edu.tau.eng.neuroscience.mri.common.datatypes.TaskImpl;
import edu.tau.eng.neuroscience.mri.common.datatypes.TaskStatus;
import edu.tau.eng.neuroscience.mri.common.exceptions.ErrorCodes;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.common.networkUtils.SSHConnection;
import edu.tau.eng.neuroscience.mri.common.networkUtils.SSHConnectionProperties;

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

    private static Logger logger = LoggerManager.getLogger(DBProxy.class);

    private Session sshSession;
    private SSHConnectionProperties sshProperties;
    private boolean useSSH;
    private Connection connection;
    private DBConnectionProperties dbProperties;
    private int localPort;

    public DBProxy(String configurationFilePath) throws DBProxyException {
        dbProperties = loadDbConfig(configurationFilePath);
        useSSH = false;
    }

    public DBProxy(String dbConfigurationFilePath, String sshConfigurationFilePath) throws DBProxyException {
        dbProperties = loadDbConfig(dbConfigurationFilePath);
        sshProperties = loadSshConfig(sshConfigurationFilePath);
        useSSH = true;
    }

    public String getUser() {
        return dbProperties.getUser();
    }

    public void connect() throws DBProxyException {
        try {
            if (connection == null || connection.isClosed()) {
                safeConnect();
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
            if (connection != null && !connection.isClosed()) {
                logger.info("Closing connection to " + getUrl() + "...");
                connection.close();
            }
            if (sshSession != null && sshSession.isConnected()) {
                logger.info("Closing SSH Connection to host: " + sshSession.getHost() + ":" + sshSession.getPort() + "...");
                sshSession.disconnect();
            }
            logger.info("Successfully closed database connection via SSH tunneling");
        } catch (SQLException e) {
            String errorMsg =
                    String.format("Failed to disconnect from the database (url: %s; user: %s).", getUrl(), getUser());
            logSqlException(e, errorMsg);
            throw new DBProxyException(ErrorCodes.DB_CONNECTION_EXCEPTION, errorMsg);
        }
    }

    public void add(List<Task> tasks) {
        // TODO add tasks to DB
    }

    public void add(Task task) {
        List<Task> tasks = new ArrayList<>();
        tasks.add(task);
        this.add(tasks);
    }

    /**
     * updates the given task in the DB (based on id)
     * @param task to update in DB.
     *             If the task ID cannot be found in the DB, this method does nothing
     */
    public void update(Task task) {
        // TODO update task in DB according to ID (decide what to do if id does not exist)
    }

    public Task getTask(int id) throws DBProxyException {
        connect();
        // TODO implement
//        Statement statement = connection.createStatement();
//        ResultSet results = statement.executeQuery("SELECT * FROM " + dbProperties.getSchema() + ".tasks WHERE id=" + id);
////        while (result.next()) {
////            int cnt = result.getInt("cnt");
////        }
//        results.close();
//        statement.close();
        return null;
    }

    private void safeConnect() throws DBProxyException {
        if (useSSH) {
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

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    String errorMsg =
                            String.format("Failed to disconnect from the database (db: %s; user: %s)",
                                    getUrl(), getUser());
                    logSqlException(e, errorMsg);
                }
            }
        });
        logger.info("Connected to DB at " + getUrl());
    }

    public List<Task> getNewTasks() throws DBProxyException {

        List<Task> tasks = new ArrayList<>();
        PreparedStatement statement = null;
        try {
            // TODO extract query to file
            statement = connection.prepareStatement(
                    "SELECT * FROM " + dbProperties.getSchema() + ".tasks WHERE status='New'");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Task task = new TaskImpl();
                task.setStatus(TaskStatus.NEW);
                task.setId(resultSet.getInt("task_id"));
                // TODO set Unit, Machine (the query should use join to get machine and unit details)
                tasks.add(task);
            }
        } catch (SQLException e) {
            String errorMsg = "Failed to retrieve new tasks from DB";
            logSqlException(e, errorMsg);
            throw new DBProxyException(ErrorCodes.QUERY_FAILURE_EXCEPTION, errorMsg);
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

}
