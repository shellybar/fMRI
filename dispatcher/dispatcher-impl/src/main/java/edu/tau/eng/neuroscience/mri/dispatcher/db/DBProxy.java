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
    private Connection connection;
    private DBConnectionProperties connProperties;
    private int localPort;

    public DBProxy(String host, int port, String schema, String user, String password) {
        connProperties = new DBConnectionProperties(host, port, schema, user, password);
    }

    public DBProxy(String configurationFilePath) throws DBProxyException {
        connProperties = loadConfig(configurationFilePath);
    }

    public String getUser() {
        return connProperties.getUser();
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
        if (sshSession != null && sshSession.isConnected()) {
            sshSession.disconnect();
        }
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
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
//        ResultSet results = statement.executeQuery("SELECT * FROM " + connProperties.getSchema() + ".tasks WHERE id=" + id);
////        while (result.next()) {
////            int cnt = result.getInt("cnt");
////        }
//        results.close();
//        statement.close();
        return null;
    }

    private void safeConnect() throws DBProxyException {
        try {
            sshSession = SSHConnection.establish();
            localPort = getFreeLocalPort();
            logger.info("SSH Tunneling to remote DB (" + connProperties.getHost() + ":" + connProperties.getPort()
                    + ") using local port " + localPort);
            sshSession.setPortForwardingL(localPort, connProperties.getHost(), connProperties.getPort());
        } catch (JSchException e) {
            String errorMsg = "Failed to establish SSH connection to the database";
            logger.error(errorMsg);
            throw new DBProxyException(ErrorCodes.SSH_CONNECTION_EXCEPTION, errorMsg);
        }
        logger.info("Establishing connection to " + getUrl() + " with user " + getUser());
        String driver = "com.mysql.jdbc.Driver";
        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(getActualUrl(), getUser(), connProperties.getPassword());
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
    }

    public List<Task> getNewTasks() throws DBProxyException {

        List<Task> tasks = new ArrayList<>();
        PreparedStatement statement = null;
        try {
            // TODO extract query to file
            statement = connection.prepareStatement(
                    "SELECT * FROM " + connProperties.getSchema() + ".tasks WHERE status='New'");
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
        return connProperties.getHost() + ":" + connProperties.getPort() + "/" + connProperties.getSchema();
    }

    private String getActualUrl() {
        return "jdbc:mysql://localhost:" + localPort + "/" + connProperties.getSchema();
    }

    private DBConnectionProperties loadConfig(String configFilePath) throws DBProxyException {
        DBConnectionProperties dbConnectionProperties = null;
        File file = new File(configFilePath);
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

    private int getFreeLocalPort() {
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
