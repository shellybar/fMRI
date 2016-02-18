package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.constants.SystemConstants;
import edu.tau.eng.neuroscience.mri.common.exceptions.MachinesManagementException;
import edu.tau.eng.neuroscience.mri.common.exceptions.QueueManagementException;
import edu.tau.eng.neuroscience.mri.dispatcher.db.DBProxy;
import edu.tau.eng.neuroscience.mri.dispatcher.db.DBProxyException;

import java.util.Properties;

public class DispatcherImpl implements Dispatcher {

    private DBProxy dbProxy;
    private UnitFetcher unitFetcher;

    public DispatcherImpl(Properties props) {
        try {
            String unitSettingsDirPath = props.getProperty(SystemConstants.UNIT_SETTINGS_FILE_PATH);
            String dbConfigFilePath = props.getProperty(SystemConstants.DB_CONFIG_FILE_PATH);
            String sshConfigFilePath = props.getProperty(SystemConstants.SSH_CONFIG_FILE_PATH);
            unitFetcher = new UnitFetcher(unitSettingsDirPath);
            dbProxy = (sshConfigFilePath == null) ?
                    new DBProxy(unitFetcher, dbConfigFilePath) :
                    new DBProxy(unitFetcher, dbConfigFilePath, sshConfigFilePath);
        } catch (DBProxyException e) {
            // TODO handle DBProxyException on dispatcher startup
        }
        try {
            QueueManager queueManager = new QueueManager(dbProxy);
        } catch (QueueManagementException e) {
            // TODO handle QueueManagementException on dispatcher startup
        } catch (MachinesManagementException e) {
            // TODO handle MachinesManagementException on dispatcher startup
        }
    }

    /**
     * A call to this function notifies the dispatcher that a fatal error has occurred
     */
    public static void notifyFatal(Throwable e) {
        // TODO notify UI, try to solve based on error code...
    }

}
