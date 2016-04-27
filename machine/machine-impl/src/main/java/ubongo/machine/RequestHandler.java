package ubongo.machine;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.io.FileUtils;
import ubongo.common.constants.MachineConstants;
import ubongo.common.constants.SystemConstants;
import ubongo.common.datatypes.RabbitData;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.TaskStatus;
import ubongo.common.exceptions.NetworkException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.networkUtils.SSHConnectionProperties;
import ubongo.common.networkUtils.SftpManager;

import java.io.*;
import java.nio.file.Paths;

/**
 * RequestHandler is called by the MachineServer when a new request arrives.
 * When a request arrive - the RequestHandler create the required objects and call the MachineControllerImpl.
 */

public class RequestHandler extends Thread {

    private static Logger logger = LogManager.getLogger(RequestHandler.class);
    private String baseDir; // The root directory where the files should be stored
    private String unitsDir; // The directory where the units should be stored, related to the base dir
    private String serverAddress; // Address of the program server
    private String configPath; // The directory where the configuration files should be stored
    private RabbitData rabbitMessage;
    private SSHConnectionProperties sshConnectionProperties;

    public RequestHandler(String threadName, RabbitData rabbitMessage, String serverAddress, String baseDir, String unitsDir, String configPath) {
        super(threadName);
        this.baseDir = baseDir;
        this.unitsDir = unitsDir;
        this.serverAddress = serverAddress;
        this.rabbitMessage = rabbitMessage;
        this.configPath = configPath;
        if (logger.isDebugEnabled()) {
            logger.debug("serverAddress = [" + serverAddress + "] baseDir = [" + baseDir + "] configPath = [" + configPath + "] " +
                    "unitsDir = [" + unitsDir + "] message = [" + rabbitMessage.getMessage() + "]");
        }
    }

    @Override
    public void run() {
        logger.debug("run() - Start");
        try {
            Configuration configuration = Configuration.loadConfiguration(configPath);
            sshConnectionProperties = configuration.getSshConnectionProperties();
            Task task = rabbitMessage.getTask();
            logger.info("Parsed request = [" + rabbitMessage.getMessage() + " " + task.getId() +"]");
            if (rabbitMessage.getMessage().equals(MachineConstants.BASE_UNIT_REQUEST)){
                String outputFilesDir = this.baseDir + File.separator  + task.getId() + MachineConstants.OUTPUT_DIR_SUFFIX;
                String inputFilesDir = this.baseDir + File.separator  + task.getId() + MachineConstants.INPUT_DIR_SUFFIX;
                handleBaseUnitRequest(inputFilesDir, outputFilesDir, task);
            } else if (rabbitMessage.getMessage().equals(MachineConstants.KILL_TASK_REQUEST)){
                handleKillRequest(task);
            } else if (rabbitMessage.getMessage().equals(MachineConstants.GET_MACHINE_PERFORMANCE)) {
                handlePerformanceRequest();
            }
        } catch (Exception e) {
            logger.error("Failed handling request: " + e.getMessage(), e);
        }
    }

    private void handlePerformanceRequest() { // TODO !!!!
    }

    private void handleKillRequest(Task task) {  // TODO !!!!
    }


    private boolean handleReceiveFiles(String inputFilesDir, String filesSourceDir){
        boolean success = true;
        logger.info("handleReceiveFiles - start. filesSourceDir= [" + filesSourceDir +
                "] from server = [" + serverAddress + "] inputFilesDir = [" + inputFilesDir + "]" );

        File inputDir = new File(inputFilesDir);
        if (inputDir.exists()) {
            logger.error("input Dir already exists...");
            return false;
        }
        boolean result = false;
        try{
            inputDir.mkdir();
            result = true;
        } catch(SecurityException se){
            logger.error("Failed to create input Dir " + inputFilesDir);
            return false;
        }
        if(!result) {
            logger.error("Failed to create input Dir " + inputFilesDir);
            return false;
        }

        SftpManager filesClient = null;
        try {
            filesClient = new SftpManager(sshConnectionProperties, serverAddress, filesSourceDir, inputFilesDir);
            filesClient.getFilesFromServer();
        } catch (NetworkException e) {
            logger.error("Failed receiving files from server " + e.getMessage());
            return false;
        }
        return success;
    }

    public void handleBaseUnitRequest(String inputFilesDir, String outputFilesDir, Task task){
        logger.info("handleBaseUnitRequest - start. task ID = [" + task.getId() +"]" );
        if (!handleReceiveFiles(inputFilesDir, task.getInputPath())){
            updateTaskFailure(task);
            return;
        }
        File outputDir = new File(outputFilesDir);
        if (outputDir.exists()) {
            logger.error("output Dir already exists... " +outputFilesDir);
            updateTaskFailure(task);
            return;
        }
        boolean result;
        try {
            outputDir.mkdir();
            result = true;
        } catch (SecurityException se){
            logger.error("Failed to create output Dir " + outputFilesDir);
            updateTaskFailure(task);
            return;
        }
        if(!result) {
            logger.error("Failed to create output Dir " + outputFilesDir);
            updateTaskFailure(task);
            return;
        }
        MachineController machineController = new MachineControllerImpl();
        boolean success = machineController.run(task, Paths.get(baseDir, unitsDir), Paths.get(baseDir));
        if (success){
            // need to send the output files to the server.
            if (sendOutputFilesToServer(task, outputFilesDir))
                updateTaskCompleted(task);
            else
                updateTaskFailure(task);
        } else {
            updateTaskFailure(task);
        }
        // delete local input & output dirs
        cleanLocalDirectories(inputFilesDir, outputFilesDir);
    }

    private void cleanLocalDirectories(String inputFilesDir, String outputFilesDir) {
        try {
            FileUtils.deleteDirectory(new File(inputFilesDir));
        } catch (IOException e) {
            logger.error("Failed cleaning local input directory " + inputFilesDir);
        }
        try {
            FileUtils.deleteDirectory(new File(outputFilesDir));
        } catch (IOException e) {
            logger.error("Failed cleaning local output directory " + outputFilesDir);
        }
    }

    private boolean sendOutputFilesToServer(Task task, String outputDir) {
        boolean success = true;
        logger.info("sendOutputFilesToServer - start. filesSourceDir= [" + outputDir +
                "] to server = [" + serverAddress + "] destination files dir = [" + task.getOutputPath() + "]" );
        SftpManager filesUploader;
        try {
            filesUploader = new SftpManager(sshConnectionProperties, serverAddress, task.getOutputPath(), outputDir);
            filesUploader.uploadFilesToServer();
        } catch (NetworkException e) {
            logger.error("Failed uploading files to server " + e.getMessage());
            return false;
        }
        return success;
    }

    private void updateTaskFailure(Task task) {
        updateTaskStatus(task, TaskStatus.FAILED);
    }

    private void updateTaskCompleted(Task task) {
        updateTaskStatus(task, TaskStatus.COMPLETED);
    }

    public void updateTaskStatus(Task task, TaskStatus status) {
        logger.info("Sending task update to server. Task id = [" + task.getId() + "]");
        final String QUEUE_NAME =  SystemConstants.UBONGO_SERVER_TASKS_STATUS_QUEUE;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(serverAddress);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            task.setStatus(status);
            RabbitData message = new RabbitData(task, MachineConstants.UPDATE_TASK_REQUEST);
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            if (logger.isDebugEnabled()) {
                logger.debug(" [!] Sent '" + message.getMessage() + "'");
            }
            channel.close();
            connection.close();
        } catch (Exception e){
            logger.error("Failed sending task status to server. Task id = [" + task.getId() + "] Status = [" +
                    status.toString() + "] error: " + e.getMessage());
        }
    }
}