package ubongo.machine;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ubongo.common.constants.MachineConstants;
import ubongo.common.datatypes.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MachineControllerImpl implements MachineController {

    private static Logger logger = LogManager.getLogger(MachineControllerImpl.class);

    @Override
    public MachineStatistics getStatistics() {
        return null;
    }

    @Override
    public boolean run(Task task, Path unitsDir) {
        String outputDir = task.getId() + MachineConstants.OUTPUT_DIR_SUFFIX;
        ProcessBuilder pb = new ProcessBuilder(getProcessCommand(task, outputDir));
        pb.directory(new File(unitsDir.toString()));
        Process p = null;
        try {
            p = pb.start();
            logger.info("Unit "+ task.getUnit().getId()+" completed successfully : " +p.getOutputStream());
        } catch (IOException e) {
            if (p != null)
                logger.error("Failed running unit: " + p.getErrorStream());
            else
                logger.error("Failed running unit: " + e.getMessage());
            return false;
        }
        return true;
    }

    private String getProcessCommand(Task task, String outputDir) {
        String inputDir = task.getId() + MachineConstants.INPUT_DIR_SUFFIX;
        String unitExecutable = Unit.getUnitFileName(task.getUnit().getId(), "sh");
        List<UnitParameter> params = task.getUnit().getParameters();
        int paramsNum = 1 + 2 + params.size();
        String[] bashCommand = new String[paramsNum];
        bashCommand[0] = unitExecutable;
        bashCommand[1] = inputDir;
        bashCommand[2] = outputDir;
        logger.debug("Unit information: Executable = " + unitExecutable + " InputDir = " + inputDir + " OutputDir = "+ outputDir);
        logger.debug("Unit arguments:");
        int i = 3;
        for (UnitParameter unitParam : params){
            bashCommand[i] = unitParam.getValue();
            logger.debug(bashCommand[i]);

            i++;
        }
        return unitExecutable;
    }

}
