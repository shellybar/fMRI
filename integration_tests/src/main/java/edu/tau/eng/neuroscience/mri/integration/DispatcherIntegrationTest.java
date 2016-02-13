package edu.tau.eng.neuroscience.mri.integration;

import edu.tau.eng.neuroscience.mri.common.constants.SystemConstants;
import edu.tau.eng.neuroscience.mri.common.datatypes.Unit;
import edu.tau.eng.neuroscience.mri.common.exceptions.DispatcherException;
import edu.tau.eng.neuroscience.mri.dispatcher.QueueManager;
import edu.tau.eng.neuroscience.mri.dispatcher.UnitFetcher;
import edu.tau.eng.neuroscience.mri.dispatcher.db.DBProxy;

public class DispatcherIntegrationTest {

    public static void main(String[] args) throws DispatcherException {
        DBProxy dbProxy = new DBProxy(
                SystemConstants.BASE_DIR + "/configs/db_connection.xml",
                SystemConstants.BASE_DIR + "/configs/ssh_connection.xml",
                true);
        QueueManager queueManager = new QueueManager(dbProxy);
        Unit unit = UnitFetcher.getUnit(1);
        unit.setParameterValues("{\"srcFile\":\"source_path\", \"destFile\":\"destination_path\"}");
        queueManager.enqueue(unit);
        //queueManager.shutdownNow();
    }

}
