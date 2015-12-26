package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.datatypes.Task;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * This class manages the communication with the DB to separate
 * queue management logic (QueueManager) and DB implementation details.
 */
public class DBProxy {

    private static Logger logger = LoggerManager.getLogger(DBProxy.class);
    private Connection connection;
    private String url;
    private String user;
    private String password;

    public DBProxy(String url, String user, String password) throws ClassNotFoundException, SQLException {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        safeConnect();
    }

    public void disconnect() throws SQLException {
        connection.close();
    }

    public void add(List<Task> tasks) {
        // TODO add tasks to DB
    }

    public void add(Task task) {
        // TODO add task to DB
    }

    /**
     * updates the given task in the DB (based on id)
     * @param task to update in DB.
     *             If the task ID cannot be found in the DB, this method does nothing
     */
    public void update(Task task) {
        // TODO update task in DB according to ID (decide what to do if id does not exist)
    }

    public Task getTask(int id) throws SQLException {
        // TODO this is just a placeholder (for example)
        assureConnection();
        Statement statement = connection.createStatement();
        ResultSet results = statement.executeQuery("SELECT * FROM Tasks WHERE id=" + id);
        // ...
        results.close();
        statement.close();
        return null;
    }

    private void assureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            safeConnect();
        }
    }

    private void safeConnect() throws SQLException {
        connection = DriverManager.getConnection(url, user, password);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    String errorMsg =
                            String.format("Failed to disconnect from the database (url: %s; user: %s)",
                                    getUrl(), getUser());
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
}
