package ubongo.machine;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import ubongo.common.constants.MachineConstants;
import ubongo.common.constants.SystemConstants;
import ubongo.common.datatypes.RabbitData;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.TaskStatus;
import ubongo.common.exceptions.NetworkException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ubongo.common.networkUtils.FilesClient;

import java.io.*;

/**
 * RequestHandler is called by the MachineServer when a new request arrives.
 * When a request arrive - the RequestHandler create the required objects and call the MachineControllerImpl.
 */

public class RequestHandler extends Thread {

    private static Logger logger = LogManager.getLogger(RequestHandler.class);
    private String baseDir; // The root directory where the files should be stored
    private String serverAddress; // Address of the program server
    private RabbitData rabbitMessage;

    public RequestHandler(RabbitData rabbitMessage, String serverAddress, String baseDir) {
        super("RequestHandler");
        this.baseDir = baseDir;
        this.serverAddress = serverAddress;
        this.rabbitMessage = rabbitMessage;
        logger.debug("serverAddress = [" + serverAddress+"] baseDir = ["+baseDir+"] message = ["+ rabbitMessage.getMessage() + "]");
    }

    @Override
    public void run() {
        logger.debug("run() - Start");
        try {
            Task task = rabbitMessage.getTask();
            logger.info("Parsed request = [" + rabbitMessage.getMessage() + " " + task.getId() +"]");

            if (rabbitMessage.getMessage().equals(MachineConstants.BASE_UNIT_REQUEST)){
                String outputFilesDir = this.baseDir + File.separator  + task.getId() + "_out";
                String inputFilesDir = this.baseDir + File.separator  + task.getId() + "_in";
                handleBaseUnitRequest(inputFilesDir, outputFilesDir, task);
            } else if (rabbitMessage.getMessage().equals(MachineConstants.KILL_TASK_REQUEST)){
                handleKillRequest(task);
            }
        } catch (Exception e) {
            logger.error("Failed handling request: " + e.getMessage());
        }
    }

    private void handleKillRequest(Task task) {
    }


    private boolean handleReceiveFiles(String inputFilesDir, String filesSourceDir){
        boolean success = true;
        logger.info("handleReceiveFiles - start. filesSourceDir= [" + filesSourceDir +
                "] from server = [" + serverAddress + "] inputFilesDir = [" + inputFilesDir + "]" );

        File inputDir = new File(inputFilesDir);
        if (inputDir.exists()) {
            logger.error("input Dir already exists..."); // TODO handle this !
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

        FilesClient filesClient = null;
        try {
            filesClient = new FilesClient(serverAddress, filesSourceDir, inputFilesDir);
            filesClient.getFilesFromServer();
        } catch (NetworkException e) {
            logger.error("Failed receiving files from server " + e.getMessage());
            return false;
        }
        return success;
    }

    public void handleBaseUnitRequest(String inputFilesDir, String outputFilesDir, Task task){
        logger.info("handleBaseUnitRequest - start. task ID = " +task.getId() +"]" );

        if (!handleReceiveFiles(inputFilesDir, task.getInputPath())){
            updateTaskFailure(task);
        }
        File outputDir = new File(outputFilesDir);
        if (outputDir.exists()) {
            logger.error("output Dir already exists..."); // TODO handle this !
            return;
        }
        boolean result = false;
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
        machineController.run(task);
    }

    private void updateTaskFailure(Task task) {
        updateTaskStatus(task, TaskStatus.FAILED);
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
            logger.debug(" [x] Sent '" + message.getMessage() + "'");
            channel.close();
            connection.close();
        } catch (Exception e){
            logger.error("Failed sending task status to server. Task id = [" + task.getId() + "] Status = [" +
                    status.toString() + "] error: " + e.getMessage());
        }
    }
}