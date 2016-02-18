package edu.tau.eng.neuroscience.mri.integration;

import edu.tau.eng.neuroscience.mri.common.constants.SystemConstants;
import edu.tau.eng.neuroscience.mri.common.datatypes.*;
import edu.tau.eng.neuroscience.mri.common.exceptions.DispatcherException;
import edu.tau.eng.neuroscience.mri.dispatcher.UnitFetcher;
import edu.tau.eng.neuroscience.mri.dispatcher.db.DBProxy;
import edu.tau.eng.neuroscience.mri.dispatcher.db.DBProxyException;

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
        // TODO clear debug tables
    }

    public static void close() throws DBProxyException {
        dbProxy.disconnect();
    }

    public static void addTaskTest() throws DispatcherException {
        Task task = new TaskImpl();
        task.setStatus(TaskStatus.NEW);
        Unit unit = unitFetcher.getUnit(1);
        unit.setParameterValues("{\"srcFile\":\"source_path\", \"destFile\":\"destination_path\"}");
        task.setUnit(unit);
        dbProxy.add(task);
        List<Task> tasks = dbProxy.getNewTasks();
        Task retrievedTask = tasks.get(0);
        assert(retrievedTask.getUnit().getId() == unit.getId());
        assert(retrievedTask.getStatus() == task.getStatus());
    }

}
