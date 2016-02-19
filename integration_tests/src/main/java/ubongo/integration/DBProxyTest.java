package ubongo.integration;

import ubongo.common.constants.SystemConstants;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.TaskImpl;
import ubongo.common.datatypes.TaskStatus;
import ubongo.common.datatypes.Unit;
import ubongo.common.exceptions.DispatcherException;
import ubongo.dispatcher.UnitFetcher;
import ubongo.dispatcher.db.DBProxy;
import ubongo.dispatcher.db.DBProxyException;

import java.util.List;


public class DBProxyTest {

    private static DBProxy dbProxy;
    private static UnitFetcher unitFetcher;

    public static void main(String[] args) throws Exception {
        init();
        addTaskTest();
        close();
    }

    public static void init() throws Exception {
        unitFetcher = new UnitFetcher(SystemConstants.BASE_DIR + "/unit_settings");
        dbProxy = new DBProxy(unitFetcher,
                SystemConstants.BASE_DIR + "/configs/db_connection.xml",
                SystemConstants.BASE_DIR + "/configs/ssh_connection.xml",
                true);
        dbProxy.connect();

        // TODO clean debug tables in DB

    }

    public static void close() throws DBProxyException {
        dbProxy.disconnect();
    }

    public static void addTaskTest() throws DispatcherException {
        Task task = new TaskImpl();
        Unit unit = unitFetcher.getUnit(1);
        unit.setParameterValues("{\"srcFile\":\"source_path\", \"destFile\":\"destination_path\"}");
        task.setUnit(unit);
        dbProxy.add(task);
        List<Task> tasks = dbProxy.getNewTasks();
        Task retrievedTask = tasks.get(0);
        assert(retrievedTask.getUnit().getId() == unit.getId());
        assert(retrievedTask.getStatus() == TaskStatus.NEW);
    }

}
