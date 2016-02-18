package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.datatypes.Machine;
import edu.tau.eng.neuroscience.mri.common.datatypes.MachineStatistics;
import edu.tau.eng.neuroscience.mri.common.datatypes.MachinesList;
import edu.tau.eng.neuroscience.mri.common.exceptions.ErrorCodes;
import edu.tau.eng.neuroscience.mri.common.exceptions.MachinesManagementException;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// TODO add unit tests
public class MachinesManager {

    private static Logger logger = LoggerManager.getLogger(MachinesManager.class);
    private final ScheduledExecutorService statisticsUpdateScheduler = Executors.newScheduledThreadPool(1);
    private static List<Machine> machines;
    private ExecutionProxy executionProxy;
    private String machinesConfigFilePath;

    public MachinesManager(ExecutionProxy executionProxy, String machinesConfigFilePath) {
        this.executionProxy = executionProxy;
        this.machinesConfigFilePath = machinesConfigFilePath;
    }

    public void start() throws MachinesManagementException {
        machines = loadMachines();
        initPeriodicalStatisticsUpdate(2, TimeUnit.HOURS);
    }

    /**
     * @return list of machines from the configuration file
     */
    public List<Machine> loadMachines() throws MachinesManagementException {
        List<Machine> machines = null;
        // TODO BASE_DIR should come from command line arguments
        File file = new File(machinesConfigFilePath);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(MachinesList.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            machines = ((MachinesList) unmarshaller.unmarshal(file)).getMachines();
        } catch (JAXBException e) {
            String originalMsg = e.getMessage();
            String msg = "Failed to parse machines configuration file: " + file.getAbsolutePath() + "" +
                    ((originalMsg == null) ? "" : ". Details: " + originalMsg);
            logger.error(msg);
        }
        if (machines == null || machines.isEmpty()) {
            throw new MachinesManagementException(ErrorCodes.FAILED_TO_LOAD_MACHINES_EXCEPTION,
                    "No machines found. Please make sure machines are configured in the server.");
        }
        return machines;
    }

    public Machine getAvailableMachine() throws MachinesManagementException {
        Machine mostAvailableMachine = null;
        float availability = 0;
        for (Machine machine: machines) {
            double currentAvailability = machine.getMachineStatistics().getAvailabilityScore();
            if (currentAvailability > availability) {
                mostAvailableMachine = machine;
            }
        }
        if (mostAvailableMachine == null) {
            // should never happen! machines list should never be null or empty.
            throw new MachinesManagementException(ErrorCodes.NO_AVAILABLE_MACHINE_EXCEPTION,
                    "No machines found. Please make sure machines are configured in the server.");
        }
        return mostAvailableMachine;
    }

    // TODO or let the machines send heartbeats?
    private void initPeriodicalStatisticsUpdate(int interval, TimeUnit intervalUnits) {
        statisticsUpdateScheduler.scheduleAtFixedRate(() -> {
            try {
                machines = loadMachines();
            } catch (MachinesManagementException e) {
                DispatcherImpl.notifyFatal(e);
                // TODO what now?
            }
            for (Machine machine: machines) {
                MachineStatistics machineStatistics = executionProxy.getStatistics(machine);
                machine.setStatistics(machineStatistics);
            }
        }, 0, interval, intervalUnits);
    }

    public List<Machine> getAllMachines() {
        return machines;
    }
}
