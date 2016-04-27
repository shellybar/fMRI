package ubongo.machine;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.constants.MachineConstants;
import ubongo.common.datatypes.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MachineControllerImpl implements MachineController {

    private static Logger logger = LogManager.getLogger(MachineControllerImpl.class);

    @Override
    public MachineStatistics getStatistics() {
        return null;
    }

    @Override
    public boolean run(Task task, Path unitsDir, Path baseDir) {
        String outputDir = task.getId() + MachineConstants.OUTPUT_DIR_SUFFIX;
        Path outputDirectory = Paths.get(baseDir.toString(), outputDir);
        ProcessBuilder pb = new ProcessBuilder(getProcessCommand(task, baseDir, outputDirectory));
        pb.directory(new File(unitsDir.toString()));
        Process p = null;
        try {
            p = pb.start();
            logger.info("Unit "+ task.getUnit().getId()+" completed successfully : " + getUnitOutput(p));
        } catch (IOException e) {
            if (p != null)
                logger.error("Failed running unit: " + getUnitErrors(p));
            else
                logger.error("Failed running unit: " + e.getMessage());
            return false;
        }
        File outputDirectoryFile = new File(outputDirectory.toString());
        if(outputDirectoryFile.list().length == 0){
            logger.error("Unit completed, but output directory is empty : " + outputDirectory.toString());
            logger.error("outputDirectoryFile.list().length : " + outputDirectoryFile.list().length);

            return false;
        }
        return true;
    }

    private String getUnitOutput(Process process) {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        try {
            while ( (line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            logger.error("Failed receiving unit output : " + e.getMessage());
        }
        return builder.toString();
    }

    private String getUnitErrors(Process process) {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        try {
            while ( (line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            logger.error("Failed receiving unit errors : " + e.getMessage());
        }
        return builder.toString();
    }

    private String[] getProcessCommand(Task task, Path baseDir, Path outputDirectory) {
        String inputDir = task.getId() + MachineConstants.INPUT_DIR_SUFFIX;
        Path inputDirectory = Paths.get(baseDir.toString(), inputDir);
        String unitExecutable = Unit.getUnitFileName(task.getUnit().getId(), ".sh");
        List<UnitParameter> params = task.getUnit().getParameters();
        int paramsNum = 1 + 2 + params.size();
        String[] bashCommand = new String[paramsNum];
        bashCommand[0] = unitExecutable;
        bashCommand[1] = inputDirectory.toString();
        bashCommand[2] = outputDirectory.toString();
        if (logger.isDebugEnabled()) {
            logger.debug("Unit information: Executable = " + unitExecutable + " InputDir = " + inputDirectory.toString() +
                    " OutputDir = " + outputDirectory.toString());
            logger.debug("Unit arguments:");
            int i = 3;
            for (UnitParameter unitParam : params) {
                bashCommand[i] = unitParam.getValue();
                logger.debug(bashCommand[i]);
                i++;
            }
        }
        return bashCommand;
    }

}
