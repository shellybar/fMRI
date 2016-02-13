package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.constants.SystemConstants;
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

    public MachinesManager(ExecutionProxy executionProxy) {
        this.executionProxy = executionProxy;
        initPeriodicalStatisticsUpdate(2, TimeUnit.HOURS);
    }

    public static void setMachines(List<Machine> machinesList) {
        machines = machinesList;
    }

    /**
     * @return list of machines from the configuration file
     */
    public static List<Machine> loadMachines() throws MachinesManagementException {
        List<Machine> machines = null;
        File file = new File(SystemConstants.BASE_DIR, "configs/machines.xml");
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
            float currentAvailability = machine.getMachineStatistics().getAvailabilityScore();
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

    private void initPeriodicalStatisticsUpdate(int interval, TimeUnit intervalUnits) {
        statisticsUpdateScheduler.scheduleAtFixedRate(() -> {
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
