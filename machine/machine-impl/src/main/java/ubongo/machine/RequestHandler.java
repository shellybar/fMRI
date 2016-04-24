package ubongo.machine;


import ubongo.common.constants.MachineConstants;
import ubongo.common.datatypes.RabbitData;
import ubongo.common.datatypes.Task;
import ubongo.common.exceptions.NetworkException;
import ubongo.common.log.Logger;
import ubongo.common.log.LoggerManager;
import ubongo.common.networkUtils.FilesClient;

import java.io.*;


/**
 * RequestHandler is called by the MachineServer when a new request arrives.
 * When a request arrive - the RequestHandler create the required objects and call the MachineControllerImpl.
 *
 * RECEIVE_FILES_REQUEST format: RECEIVE_FILES_REQUEST CONNECTION_PORT NUM_OF_FILES
 * BASE_UNIT_REQUEST format:  TODO
 *
 */

public class RequestHandler extends Thread {

    private static Logger logger = LoggerManager.getLogger(RequestHandler.class);
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
            }
        } catch (Exception e) {
            logger.error("Failed handling request: " + e.getMessage());
        }
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
        }
        catch(SecurityException se){
            logger.error("Failed to create input Dir " + inputFilesDir); // TODO handle this !
            return false;
        }
        if(!result) {
            logger.error("Failed to create input Dir " + inputFilesDir); // TODO handle this !
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
            // TODO - update task failure!
            return;
        }

        File outputDir = new File(outputFilesDir);
        if (outputDir.exists()) {
            logger.error("output Dir already exists..."); // TODO handle this !
            return;
        }
        boolean result = false;
        try{
            outputDir.mkdir();
            result = true;
        }
        catch(SecurityException se){
            logger.error("Failed to create input Dir " + outputFilesDir); // TODO handle this !
            return;
        }
        if(!result) {
            logger.error("Failed to create input Dir " + outputFilesDir); // TODO handle this !
            return;
        }

        MachineController machineController = new MachineControllerImpl();
        machineController.run(task);

    }

}