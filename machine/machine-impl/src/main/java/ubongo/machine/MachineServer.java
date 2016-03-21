package ubongo.machine;

import com.sun.xml.internal.messaging.saaj.soap.Envelope;
import ubongo.common.constants.MachineConstants;
import ubongo.common.constants.SystemConstants;
import ubongo.common.datatypes.MachineStatistics;
import ubongo.common.datatypes.RabbitData;
import ubongo.common.datatypes.Task;
import ubongo.common.log.Logger;
import ubongo.common.log.LoggerManager;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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

    private List<Task> runningTasks;
    private MachineStatistics machineStatistics;

    public MachineServer() {
        runningTasks = new ArrayList<>();
        this.machineStatistics = new MachineStatistics(runningTasks);
    }

    public void start() {
        trackMachinePerformance();
    }

    public static void main(String[] args) {

        logger.info("Initializing machine-server...");
        MachineServer machineServer = new MachineServer();
        // machineServer.start(); // TODO remove comment
        final String QUEUE_NAME = SystemConstants.UBONGO_RABBIT_QUEUE;

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, com.rabbitmq.client.Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    String serverAddress = MachineConstants.SERVER_FALLBACK;
                    try {
                        RabbitData message = RabbitData.fromBytes(body);
                        System.out.println(" [x] Received '" + message.getMessage() + "'");
                        String baseDir = "";
                        Properties props = parseCommandLineArgs(args);
                        baseDir = props.getProperty(ARG_DIR);
                        serverAddress = props.getProperty(ARG_SERVER);
                        logger.info("Server address: [" + serverAddress + "], base directory path: [" + baseDir + "]");
                        RequestHandler requestHandler = new RequestHandler(message, serverAddress, baseDir);
                        logger.debug("Starting RequestHandler thread...");
                        requestHandler.start();
                    } catch (Exception e){
                        throw new IOException(e);
                    }
                }
            };
            channel.basicConsume(QUEUE_NAME, true, consumer);
        } catch (Exception e){
            logger.error("Failed receiving message via rabbit mq error: " + e.getMessage());
        }


    }

    private void trackMachinePerformance() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable sampler = new MachinePerformanceSampler(machineStatistics);
        scheduler.scheduleAtFixedRate(sampler, 0, 30, TimeUnit.SECONDS);

    }

    private static Properties parseCommandLineArgs(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption(buildOption("Server Address", "The IP or host name of the server", ARG_SERVER));
        options.addOption(buildOption("Base Directory", "The path to the directory where filed will be stored", ARG_DIR));
        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(options, args);

        Properties result = new Properties();
        result.setProperty(ARG_SERVER, line.getOptionValue(ARG_SERVER, MachineConstants.SERVER_FALLBACK));
        result.setProperty(ARG_DIR, line.getOptionValue(ARG_DIR, ""));
        return result;
    }

    private static Option buildOption(String argName, String description, String argLabel) {
        return Option.builder(argLabel).hasArg().argName(argName).desc(description).required(false).build();
    }
}