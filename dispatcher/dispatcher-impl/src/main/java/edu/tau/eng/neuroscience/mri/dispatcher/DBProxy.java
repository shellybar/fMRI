package edu.tau.eng.neuroscience.mri.dispatcher;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import edu.tau.eng.neuroscience.mri.common.datatypes.Task;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.common.networkUtils.SSHConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages the communication with the DB to separate
 * queue management logic (QueueManager) and DB implementation details.
 */
public class DBProxy {

    private static Logger logger = LoggerManager.getLogger(DBProxy.class);

    private Connection connection;
    private String mySqlHost;
    private String schema;
    private String user;
    private String password;
    private int mySqlServerPort;

    private int localPort = 49999; // TODO find free local port?

    public DBProxy(String host, int port, String schema, String user, String password)
            throws ClassNotFoundException, SQLException {
        this.mySqlHost = host;
        this.mySqlServerPort = port;
        this.schema = schema;
        this.user = user;
        this.password = password;
    }

    //TODO create DBConnectionProperties class
//    public DBProxy(DBConnectionProperties connectionProperties) throws ClassNotFoundException, SQLException {
//
//    }

    public String getUser() {
        return user;
    }

    public void connect() throws SQLException, JSchException {
        if (connection == null || connection.isClosed()) {
            safeConnect();
        }
    }

    public void disconnect() throws SQLException {
        connection.close();
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

    public Task getTask(int id) throws SQLException, JSchException, ClassNotFoundException {
        // TODO this is just a placeholder (for example)
        assureConnection();
        Statement statement = connection.createStatement();
        ResultSet results = statement.executeQuery("SELECT * FROM Tasks WHERE id=" + id);
//        while (result.next()) {
//            int cnt = result.getInt("cnt");
//        }
        results.close();
        statement.close();
        return null;
    }

    private void assureConnection() throws SQLException, JSchException, ClassNotFoundException {
        if (connection == null || connection.isClosed()) {
            safeConnect();
        }
    }

    // TODO improve exceptions
    private void safeConnect() throws SQLException, JSchException {

        Session sshSession = SSHConnection.establish();
        sshSession.setPortForwardingL(localPort, mySqlHost, mySqlServerPort);
        logger.info("Establishing connection to " + getUrl() + " with user " + user);
        String driver = "com.mysql.jdbc.Driver";
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database connection cannot be established. " +
                    "MySQL JDBC driver class (" + driver + ") was not found");
        }
        connection = DriverManager.getConnection(getUrl(), user, password);

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
                                    getUrl(), user);
                    logger.error(errorMsg +
                            "\nSQLException: " + e.getMessage() +
                            "\nSQLState: " + e.getSQLState() +
                            "\nVendorError: " + e.getErrorCode());
                }
            }
        });
    }

    public List<Task> getNewTasks() {
        return null; // TODO
    }

    public String getUrl() {
        return "jdbc:mysql://localhost:" + localPort + "/" + schema;
    }

}
