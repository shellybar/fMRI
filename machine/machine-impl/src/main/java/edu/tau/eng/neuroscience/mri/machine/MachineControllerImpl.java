package edu.tau.eng.neuroscience.mri.machine;


import edu.tau.eng.neuroscience.mri.common.datatypes.MachineStatistics;
import edu.tau.eng.neuroscience.mri.common.datatypes.Unit;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;

public class MachineControllerImpl implements MachineController {

    private static Logger logger = LoggerManager.getLogger(MachineControllerImpl.class);

    @Override
    public MachineStatistics getStatistics() {
        logger.info("Example logging...");
        return null;
    }

    @Override
    public void run(Unit unit) {

    }
}
