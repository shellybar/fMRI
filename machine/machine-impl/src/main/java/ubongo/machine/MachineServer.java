package ubongo.machine;

import ubongo.common.constants.MachineConstants;
import ubongo.common.constants.SystemConstants;
import ubongo.common.datatypes.MachineStatistics;
import ubongo.common.datatypes.RabbitData;
import ubongo.common.datatypes.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.*;

/**
 * MachineServer run on each machine all the time, and listens to socket requests.
 * When a request arrives - the MachineServer creates the required objects and call the MachineControllerImpl.
 */
public class MachineServer {

    public static final String ARG_SERVER = "server";
    public static final String ARG_DIR = "base_dir";
    public static final String ARG_UNITS = "units_dir";
    private static final String CONFIG_PATH = "config";

    private static Logger logger = LogManager.getLogger(MachineServer.class);

    private List<Task> runningTasks;
    private MachineStatistics machineStatistics;
    static String serverAddress;

    public MachineServer() {
        runningTasks = new ArrayList<>();
        this.machineStatistics = new MachineStatistics(runningTasks);
        serverAddress = MachineConstants.SERVER_FALLBACK;
    }

    public void start() {
        trackMachinePerformance();
    }

    public static void main(String[] args) {

        logger.info("Initializing machine-server...");
        MachineServer machineServer = new MachineServer();
        // machineServer.start(); // TODO remove comment
        final String TASKS_QUEUE_NAME = SystemConstants.UBONGO_RABBIT_TASKS_QUEUE;
        final String KILL_TASKS_QUEUE_NAME = SystemConstants.UBONGO_RABBIT_KILL_TASKS_QUEUE;
        try {
            System.out.println(" [*] Waiting for new tasks. To exit press CTRL+C");
            tasksListener(TASKS_QUEUE_NAME, '+');
            tasksListener(KILL_TASKS_QUEUE_NAME, 'x');
        } catch (Exception e){
            logger.error("Failed receiving message via rabbit mq error: " + e.getMessage());
        }
    }

    private static void tasksListener(String queue, char actionSign) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(queue, false, false, false, null);
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                try {
                    RabbitData message = RabbitData.fromBytes(body);
                    System.out.println(" ["+actionSign+"] Received '" + message.getMessage() + "'");
                    String baseDir = System.getProperty(ARG_DIR);
                    String unitsDir = System.getProperty(ARG_UNITS);
                    serverAddress = System.getProperty(ARG_SERVER);
                    String configPath = System.getProperty(CONFIG_PATH);
                    logger.info("Server address: [" + serverAddress + "], base directory path: [" + baseDir + "]");
                    RequestHandler requestHandler = new RequestHandler(message, serverAddress, baseDir, unitsDir, configPath);
                    logger.debug("Starting RequestHandler thread...");
                    requestHandler.start();
                } catch (Exception e){
                    throw new IOException(e);
                }
            }
        };
        channel.basicConsume(queue, true, consumer);
    }

    private void trackMachinePerformance() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable sampler = new MachinePerformanceSampler(machineStatistics);
        scheduler.scheduleAtFixedRate(sampler, 0, 30, TimeUnit.SECONDS);

    }

    private static Properties parseCommandLineArgs(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption(buildOption("Server Address", "The IP or host name of the server", ARG_SERVER));
        options.addOption(buildOption("Base Directory", "The path to the directory where files will be stored", ARG_DIR));
        options.addOption(buildOption("Units Directory", "The path to the directory where unit scripts will be stored, relative to "+ARG_DIR, ARG_UNITS));

        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(options, args);

        Properties result = new Properties();
        result.setProperty(ARG_SERVER, line.getOptionValue(ARG_SERVER, MachineConstants.SERVER_FALLBACK));
        result.setProperty(ARG_DIR, line.getOptionValue(ARG_DIR, ""));
        result.setProperty(ARG_UNITS, line.getOptionValue(ARG_UNITS, "Bash"));

        return result;
    }

    private static Option buildOption(String argName, String description, String argLabel) {
        return Option.builder(argLabel).hasArg().argName(argName).desc(description).required(false).build();
    }
}