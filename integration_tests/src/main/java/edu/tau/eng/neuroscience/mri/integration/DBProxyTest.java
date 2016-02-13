package edu.tau.eng.neuroscience.mri.integration;

import edu.tau.eng.neuroscience.mri.common.constants.SystemConstants;
import edu.tau.eng.neuroscience.mri.common.datatypes.*;
import edu.tau.eng.neuroscience.mri.common.exceptions.DispatcherException;
import edu.tau.eng.neuroscience.mri.dispatcher.db.DBProxy;
import edu.tau.eng.neuroscience.mri.dispatcher.db.DBProxyException;


public class DBProxyTest {

    private static DBProxy dbProxy;

    public static void main(String[] args) throws Exception {
        init();
        addTaskTest();
        clean();
    }

    public static void init() throws Exception {
        dbProxy = new DBProxy(
                SystemConstants.BASE_DIR + "/configs/db_connection.xml",
                SystemConstants.BASE_DIR + "/configs/ssh_connection.xml",
                true);
        dbProxy.connect();
    }

    public static void clean() throws DBProxyException {
        // TODO clear debug tables
        dbProxy.disconnect();
    }

    public static void addTaskTest() throws DispatcherException {
        Task task = new TaskImpl();
        task.setStatus(TaskStatus.NEW);
        Unit unit = new BaseUnit();
        unit.setId(1);
        task.setUnit(unit);
        dbProxy.add(task);
        //dbProxy.getTask(taskId);
        // TODO assert and delete from db
    }

}
