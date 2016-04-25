package ubongo.execution;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.MachineStatistics;
import ubongo.common.datatypes.MachinesList;
import ubongo.execution.exceptions.MachinesManagementException;

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

    private static Logger logger = LogManager.getLogger(MachinesManager.class);
    private final ScheduledExecutorService statisticsUpdateScheduler = Executors.newScheduledThreadPool(1);
    private List<Machine> machines;
    private ExecutionProxy executionProxy;

    MachinesManager(List<Machine> machines, ExecutionProxy executionProxy) {
        this.executionProxy = executionProxy;
        this.machines = machines;
    }

    public void start() {
        initPeriodicalStatisticsUpdate(2, TimeUnit.HOURS);
    }

    public void stop() {
        statisticsUpdateScheduler.shutdownNow();
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
            throw new MachinesManagementException(
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
