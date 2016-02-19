package ubongo.machine;


import ubongo.common.datatypes.MachineStatistics;
import ubongo.common.datatypes.Unit;
import ubongo.common.log.Logger;
import ubongo.common.log.LoggerManager;

// TODO what is this class good for if we have the MachineServer?
public class MachineControllerImpl implements MachineController {

    private static Logger logger = LoggerManager.getLogger(MachineControllerImpl.class);

    @Override
    public MachineStatistics getStatistics() {
        return null;
    }

    @Override
    public void run(Unit unit) {

    }
}
