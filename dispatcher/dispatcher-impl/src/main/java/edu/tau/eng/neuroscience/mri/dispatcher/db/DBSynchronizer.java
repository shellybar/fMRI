package edu.tau.eng.neuroscience.mri.dispatcher.db;


import edu.tau.eng.neuroscience.mri.common.datatypes.Machine;
import edu.tau.eng.neuroscience.mri.common.datatypes.Unit;
import edu.tau.eng.neuroscience.mri.common.exceptions.ErrorCodes;
import edu.tau.eng.neuroscience.mri.common.exceptions.MachinesManagementException;
import edu.tau.eng.neuroscience.mri.common.exceptions.SynchronizationException;
import edu.tau.eng.neuroscience.mri.common.exceptions.UnitFetcherException;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.dispatcher.Dispatcher;
import edu.tau.eng.neuroscience.mri.dispatcher.DispatcherImpl;
import edu.tau.eng.neuroscience.mri.dispatcher.MachinesManager;
import edu.tau.eng.neuroscience.mri.dispatcher.UnitFetcher;
import edu.tau.eng.neuroscience.mri.server.AnalysesServer;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DBSynchronizer {

    private static Logger logger = LoggerManager.getLogger(DBSynchronizer.class);
    private final ScheduledExecutorService syncScheduler = Executors.newScheduledThreadPool(1);
    private DBProxy dbProxy;

    public DBSynchronizer(DBProxy dbProxy) {
        this.dbProxy = dbProxy;
        initSynchronizer(5, TimeUnit.HOURS);
    }

    private void initSynchronizer(int interval, TimeUnit intervalUnits) {
        syncScheduler.scheduleAtFixedRate(() -> {
            try {
                updateMachinesInDatabase();
                updateUnitsInDatabase();
            } catch (SynchronizationException e) {
                logger.fatal(e.getMessage());
                DispatcherImpl.notifyFatal(e);
            }
        }, 0, interval, intervalUnits);
    }

    /**
     * The database should store the machines details and statistics.
     * The mission of this method is to sync between the database and the machines configuration file,
     * which might be updated, even when the program is running.
     */
    private void updateMachinesInDatabase() throws SynchronizationException {
        try {
            List<Machine> machines = MachinesManager.loadMachines();
            MachinesManager.setMachines(machines);
            dbProxy.updateMachines(machines);
        } catch (MachinesManagementException | DBProxyException e) {
            throw new SynchronizationException(ErrorCodes.MACHINES_SYNCHRONIZATION_EXCEPTION,
                    "Synchronization failed: " + e.getMessage());
        }
    }

    /**
     * The database should store the units details.
     * The mission of this method is to sync between the database and the units configuration files,
     * which might be updated, even when the program is running.
     */
    private void updateUnitsInDatabase() throws SynchronizationException {
        try {
            List<Unit> units = UnitFetcher.getAllUnits();
            dbProxy.updateUnits(units);
        } catch (UnitFetcherException | DBProxyException e) {
            throw new SynchronizationException(ErrorCodes.UNITS_SYNCHRONIZATION_EXCEPTION,
                    "Synchronization failed: " + e.getMessage());
        }
    }
}
