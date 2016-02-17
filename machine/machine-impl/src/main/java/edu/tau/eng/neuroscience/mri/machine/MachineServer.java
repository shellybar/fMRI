package edu.tau.eng.neuroscience.mri.machine;

import edu.tau.eng.neuroscience.mri.common.constants.MachineConstants;
import edu.tau.eng.neuroscience.mri.common.datatypes.MachineStatistics;
import edu.tau.eng.neuroscience.mri.common.datatypes.Task;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.*;

/**
 * MachineServer run on each machine all the time, and listens to socket requests.
 * When a request arrives - the MachineServer creates the required objects and call the MachineControllerImpl.
 */
public class MachineServer {

    public static final String ARG_SERVER = "server";
    public static final String ARG_DIR = "basedir";
    private static Logger logger = LoggerManager.getLogger(MachineServer.class);

    private List<Task> runningTasks = new ArrayList<>();
    private MachineStatistics machineStatistics;

    public MachineServer() {
        this.machineStatistics = new MachineStatistics(runningTasks);
        trackMachinePerformance();
    }

    public static void main(String[] args) {

        logger.info("Initializing machine-server...");

        MachineServer machineServer = new MachineServer();

        String serverAddress = MachineConstants.SERVER_FALLBACK;
        String baseDir = "";
        try {
            HashMap<String, String> map = parseCommandLineArgs(args);
            serverAddress = map.get(ARG_SERVER);
            baseDir = map.get(ARG_DIR);
        } catch (ParseException e) {
            logger.error("Failed to parse command line arguments - continuing with default values");
        }
        logger.info("Server address: [" + serverAddress + "], base directory path: [" + baseDir + "]");

        try (ServerSocket serverSocket = new ServerSocket(MachineConstants.MACHINE_LISTENING_PORT)) {
            logger.debug("Initialized new socket on port: [" + MachineConstants.MACHINE_LISTENING_PORT + "]");
            logger.info("Listening on port: " + MachineConstants.MACHINE_LISTENING_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.debug("Accepted a new socket");
                RequestHandler requestHandler = new RequestHandler(clientSocket, serverAddress, baseDir);
                logger.debug("Starting RequestHandler thread...");
                requestHandler.start();
            }
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Machine server exception: " + e.getMessage());
        }
    }

    private void trackMachinePerformance() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable sampler = new MachinePerformanceSampler(machineStatistics);
        scheduler.scheduleAtFixedRate(sampler, 0, 30, TimeUnit.SECONDS);

    }

    private static HashMap<String, String> parseCommandLineArgs(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption(buildOption("Server Address", "The IP or host name of the server", ARG_SERVER));
        options.addOption(buildOption("Base Directory", "The path to the directory where filed will be stored", ARG_DIR));
        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(options, args);

        HashMap<String, String> result = new HashMap<>();
        result.put(ARG_SERVER, line.getOptionValue(ARG_SERVER, MachineConstants.SERVER_FALLBACK));
        result.put(ARG_DIR, line.getOptionValue(ARG_DIR, ""));
        return result;
    }

    private static Option buildOption(String argName, String description, String argLabel) {
        return Option.builder(argLabel).hasArg().argName(argName).desc(description).required(false).build();
    }
}