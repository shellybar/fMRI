package edu.tau.eng.neuroscience.mri.machine;

import edu.tau.eng.neuroscience.mri.common.datatypes.MachineStatistics;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import org.hyperic.sigar.*;

public class MachinePerformanceSampler implements Runnable {

    private static Logger logger = LoggerManager.getLogger(MachinePerformanceSampler.class);
    private MachineStatistics machineStatistics;

    public MachinePerformanceSampler(MachineStatistics machineStatistics) {
        this.machineStatistics = machineStatistics;
    }

    @Override
    public void run() {
        Sigar sigar = new Sigar();
        try {
            double totalCpuUsagePercent = sigar.getCpuPerc().getCombined();
            machineStatistics.updateCpuUsage(totalCpuUsagePercent);
            machineStatistics.updateMemoryUsage(sigar.getMem().getUsedPercent() / 100.0);
            logger.debug("CPU: " + Math.round(machineStatistics.getCpuUsage() * 10000.0) / 100.0 + "%" +
                    " Mem: " + Math.round(machineStatistics.getMemoryUsage() * 10000.0) / 100.0 + "%");
        } catch (SigarException e) {
            logger.error("Failed to sample machine performance. Details: " + e.getMessage());
        } finally {
            sigar.close();
        }
    }
}
