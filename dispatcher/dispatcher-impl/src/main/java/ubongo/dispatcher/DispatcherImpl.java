package ubongo.dispatcher;

import ubongo.common.constants.SystemConstants;
import ubongo.common.datatypes.Context;
import ubongo.common.datatypes.Unit;
import ubongo.common.exceptions.DispatcherException;
import ubongo.common.exceptions.MachinesManagementException;
import ubongo.common.exceptions.QueueManagementException;
import ubongo.dispatcher.db.DBProxy;
import ubongo.dispatcher.db.DBProxyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DispatcherImpl implements Dispatcher {

    private DBProxy dbProxy;
    private UnitFetcher unitFetcher;
    private MachinesManager machinesManager;
    private QueueManager queueManager;
    private ExecutionProxy executionProxy;
    private FlowVerificator flowVerificator;

    public DispatcherImpl(Properties props) {
        this(props, false);
    }

    public DispatcherImpl(Properties props, boolean debug) {
        String unitSettingsDirPath = props.getProperty(SystemConstants.UNIT_SETTINGS_DIR_PATH);
        String dbConfigFilePath = props.getProperty(SystemConstants.DB_CONFIG_FILE_PATH);
        String sshConfigFilePath = props.getProperty(SystemConstants.SSH_CONFIG_FILE_PATH);
        String machinesConfigFilePath = props.getProperty(SystemConstants.MACHINES_CONFIG_FILE_PATH);

        executionProxy = ExecutionProxy.getInstance();
        unitFetcher = new UnitFetcher(unitSettingsDirPath);
        try {
            dbProxy = (sshConfigFilePath == null) ?
                    new DBProxy(unitFetcher, dbConfigFilePath, debug) :
                    new DBProxy(unitFetcher, dbConfigFilePath, sshConfigFilePath, debug);
        } catch (DBProxyException e) {
            // TODO handle exception on Dispatcher startup
        }
        machinesManager = new MachinesManager(executionProxy, machinesConfigFilePath);
        queueManager = new QueueManager(dbProxy, machinesManager);
        flowVerificator = new FlowVerificator(queueManager);
    }

    @Override
    public void start() {
        try {
            dbProxy.start();
            machinesManager.start();
            queueManager.start();
        } catch (QueueManagementException e) {
            // TODO handle QueueManagementException on dispatcher start
        } catch (DBProxyException e) {
            // TODO handle DBProxyException on dispatcher start
        } catch (MachinesManagementException e) {
            // TODO handle MachinesManagementException on dispatcher start
        }
    }

    @Override
    public void dispatch(Context context, List<Unit> units) {
        try {
            List<Unit> returnedUnits = flowVerificator.startAnalysis(context, units);
            if (units == returnedUnits) {
                // TODO
            } else {
                // TODO
            }
        } catch (DispatcherException e) {
            // TODO handle DispatcherException
        }
    }

    @Override
    public void dispatch(Context context, Unit unit) {
        List<Unit> units = new ArrayList<>(1);
        units.add(unit);
        dispatch(context, units);
    }

    /**
     * A call to this function notifies the dispatcher that a fatal error has occurred
     */
    public static void notifyFatal(Throwable e) {
        // TODO notify UI, try to solve based on error code...
    }
}
