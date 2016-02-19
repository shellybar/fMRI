package ubongo.server;

import ubongo.common.datatypes.BaseUnit;
import ubongo.common.datatypes.Unit;
import ubongo.common.log.Logger;
import ubongo.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.dispatcher.*;

import java.io.File;
import java.util.List;
import java.util.Properties;
import org.apache.commons.cli.*;
import ubongo.dispatcher.Dispatcher;
import ubongo.dispatcher.DispatcherImpl;

public class AnalysesServer {

    private static Logger logger = LoggerManager.getLogger(AnalysesServer.class);

    public static void main(String[] args) throws ParseException {

        logger.info("Start...");
        Properties props = parseCommandLineArgs(args);
        Dispatcher dispatcher = new DispatcherImpl(props);
        dispatcher.start();

        Unit unit = new BaseUnit();
        unit.setId(1);
        unit.setParameterValues("{}"); // TODO unit.setInputPath("serverWorkspace"); change this protocol - pass a task
        dispatcher.dispatch(null, unit);
    }

    public void submitAnalysis(List<Integer> unitIds) {
        return;
    }

    private static Properties parseCommandLineArgs(String[] args) throws ParseException {

        Options options = new Options();
        options.addOption(buildOption("Unit Settings Directory", "The path to the directory where unit settings are stored", UNIT_SETTINGS_DIR_PATH));
        options.addOption(buildOption("DB Configuration File Path", "The path to the database connection configuration file", DB_CONFIG_FILE_PATH));
        options.addOption(buildOption("SSH Configuration File Path", "The path to the SSH connection configuration file (optional)", SSH_CONFIG_FILE_PATH));
        options.addOption(buildOption("Machines Configuration File Path", "The path to the machines configuration file", MACHINES_CONFIG_FILE_PATH));
        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(options, args);

        Properties result = new Properties();
        result.setProperty(UNIT_SETTINGS_DIR_PATH, line.getOptionValue(UNIT_SETTINGS_DIR_PATH,
                new File(BASE_DIR.getAbsolutePath(), "unit_settings").getAbsolutePath()));
        result.setProperty(DB_CONFIG_FILE_PATH, line.getOptionValue(DB_CONFIG_FILE_PATH,
                new File(BASE_DIR.getAbsolutePath(), "configs/db_connection.xml").getAbsolutePath()));
        result.setProperty(SSH_CONFIG_FILE_PATH, line.getOptionValue(SSH_CONFIG_FILE_PATH,
                new File(BASE_DIR.getAbsolutePath(), "configs/ssh_connection.xml").getAbsolutePath()));
        result.setProperty(MACHINES_CONFIG_FILE_PATH, line.getOptionValue(MACHINES_CONFIG_FILE_PATH,
                new File(BASE_DIR.getAbsolutePath(), "configs/machines.xml").getAbsolutePath()));
        return result;
    }

    private static Option buildOption(String argName, String description, String argLabel) {
        return Option.builder(argLabel).hasArg().argName(argName).desc(description).required(false).build();
    }

}
