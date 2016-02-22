package ubongo.dispatcher;


import ubongo.common.constants.MachineConstants;
import ubongo.common.constants.SystemConstants;
import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.MachineStatistics;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.TaskImpl;
import ubongo.common.log.Logger;
import ubongo.common.log.LoggerManager;
import ubongo.common.networkUtils.FilesServer;

import java.io.*;
import java.net.Socket;

public enum ExecutionProxy {

    INSTANCE; // This is a singleton

    public static ExecutionProxy getInstance() {
        return INSTANCE;
    }

    private static Logger logger = LoggerManager.getLogger(ExecutionProxy.class);
    private FilesServer server;
    private String inputDirPath;
    private QueueManager queueManager;

    /**
     * @param task to execute
     * @param queueManager to send the task back after execution
     */
    public void execute(Task task, QueueManager queueManager) {
        this.queueManager = queueManager;
        int returnCode = sendInputFiles(task, SystemConstants.BASE_DIR.getAbsolutePath());
        if (returnCode==MachineConstants.RECEIVE_ERROR) {
            // TODO handle it! update the queque manager about the failure
        }

        returnCode = excecuteTaskOnTheMachine(task);
        /* TODO continue - check return code etc. */



        // TODO move this part of the code to the method called by the client after execution (it's here only to test the dispatcher)
        //task.setStatus(TaskStatus.COMPLETED);
        //queueManager.updateTaskAfterExecution(task);
    }

    public MachineStatistics getStatistics(Machine machine) {
        return new MachineStatistics(null); // TODO
    }

    public int sendInputFiles(Task task, String baseDir) {

        logger.info("Starting to send input files. Task id = [" + task.getId() + "]");
        int connectionPort = task.getNetworkPort();
        server = new FilesServer(connectionPort, baseDir);
        inputDirPath = task.getInputPath();
        logger.info("Input path: " + inputDirPath);
        int numberOfInputFiles = server.getFilesInDir(inputDirPath).length;
        logger.info("Number of input files: " + numberOfInputFiles);

        // Start the process for sending files on a new thread, and start the client on the designated machine.

        Runnable myRunnableServer = () -> server.manageFileTransfer(inputDirPath);
        Thread thread = new Thread(myRunnableServer);
        thread.start();

        // Start the client on the designated machine and wait for response.
        return sendGetFilesRequest(task, connectionPort, numberOfInputFiles);
    }

    public int excecuteTaskOnTheMachine(Task task) {

        logger.info("Sending request to run unit on the machine. Task id = [" + task.getId() + "]");
        int connectionPort = task.getNetworkPort();
        int returnCode = sendBaseUnitRequest( task, connectionPort);

        // TODO maybe check return code here... or upate queue manager etc...
        return returnCode;
    }

    /**
     * Sends remote request to the machine using socket
     *
     * RECEIVE_FILES_REQUEST format: RECEIVE_FILES_REQUEST CONNECTION_PORT NUM_OF_FILES
     * BASE_UNIT_REQUEST format:  BASE_UNIT_REQUEST TASK_OBJECT
     *
     */
    private int sendGetFilesRequest(Task task, int connectionPort, int numberOfInputFiles) {
        String request = MachineConstants.RECEIVE_FILES_REQUEST + " " + connectionPort + " " + numberOfInputFiles;
        char requestLenInChar = (char) request.length();
        String requestWithLength = requestLenInChar + request;
        logger.info("Sending request: " + requestWithLength);
        Socket socket = null;

        int success=0;
        int retries=0;
        while ((success == 0) && (retries < SystemConstants.NETWORK_RETRIES)) {
            if (retries>0){
                try {
                    Thread.sleep(SystemConstants.SLEEP_BETWEEN_NETWORK_RETRIES);
                } catch (InterruptedException e1) {
                    // do nothing
                }
            }
            retries++;
            try {
                socket = new Socket(task.getMachine().getAddress(), MachineConstants.MACHINE_LISTENING_PORT);
                logger.info("Initialized socket to designated machine: ip=[" + task.getMachine().getAddress()
                        + "], port=[" + MachineConstants.MACHINE_LISTENING_PORT + "]");
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(requestWithLength.getBytes());
                outputStream.flush();
                success=1;
            } catch (IOException e) {
                logger.error("Error during sendGetFilesRequest: " + e.getMessage());
                if (socket != null)
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        // do nothing
                    }
            }
        }
        if (success==1)
           return MachineConstants.RECEIVE_ERROR;
        try{
            logger.info("Initialized socket to designated machine: ip=[" + task.getMachine().getAddress()
                    + "], port=[" + MachineConstants.MACHINE_LISTENING_PORT + "]");
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(requestWithLength.getBytes());
            outputStream.flush();

            // Now wait for response

            byte[] byteArray = new byte[1];
            DataInputStream dataInputStream =
                    new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            int bytesRead = dataInputStream.read(byteArray, 0, byteArray.length);
            if (bytesRead < 0){
                logger.error("Failed receive machine's response");
                return MachineConstants.RECEIVE_ERROR;
            } else {
                int returnCode = byteArray[0];
                if (returnCode == MachineConstants.RECEIVED_ALL_FILES) {
                    logger.info("All files received successfully!");
                } else {
                    logger.error("Error occurred while sending files.");
                }
                return returnCode;
            }
        } catch (IOException e) {
            logger.error("Error during sendGetFilesRequest: " + e.getMessage());
            return MachineConstants.RECEIVE_ERROR;
        } finally {
            if (socket!=null)
                try {
                    socket.close();
                } catch (IOException e1) {
                    // do nothing
                }
        }
    }


    /**
     * Sends remote request to the machine using socket
     *
     * RECEIVE_FILES_REQUEST format: RECEIVE_FILES_REQUEST CONNECTION_PORT NUM_OF_FILES
     * BASE_UNIT_REQUEST format:  BASE_UNIT_REQUEST CONNECTION_PORT TASK_OBJECT
     *
     */
    private int sendBaseUnitRequest(Task task, int connectionPort) {
        String request = MachineConstants.BASE_UNIT_REQUEST + " " + connectionPort;
        char requestLenInChar = (char) request.length();
        String requestWithLength = requestLenInChar + request;
        logger.info("Sending request: " + requestWithLength);
        Socket socket = null;

        int success=0;
        int retries=0;
        while ((success == 0) && (retries < SystemConstants.NETWORK_RETRIES)) {
            if (retries>0){
                try {
                    Thread.sleep(SystemConstants.SLEEP_BETWEEN_NETWORK_RETRIES);
                } catch (InterruptedException e1) {
                    // do nothing
                }
            }
            retries++;
            try {
                socket = new Socket(task.getMachine().getAddress(), MachineConstants.MACHINE_LISTENING_PORT);
                logger.info("Initialized socket to designated machine: ip=[" + task.getMachine().getAddress()
                        + "], port=[" + MachineConstants.MACHINE_LISTENING_PORT + "]");
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(requestWithLength.getBytes());
                outputStream.flush();

                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(task);
                objectOutputStream.flush();

                success=1;
            } catch (IOException e) {
                logger.error("Error during sendBaseUnitRequest: " + e.getMessage());
                if (socket != null)
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        // do nothing
                    }
            }
        }
        if (success==1)
            return MachineConstants.BASE_UNIT_FAILURE;
        try{
            logger.info("Initialized socket to designated machine: ip=[" + task.getMachine().getAddress()
                    + "], port=[" + MachineConstants.MACHINE_LISTENING_PORT + "]");
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(requestWithLength.getBytes());
            outputStream.flush();

            // Now wait for response

            byte[] byteArray = new byte[1];
            DataInputStream dataInputStream =
                    new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            int bytesRead = dataInputStream.read(byteArray, 0, byteArray.length);
            if (bytesRead < 0){
                logger.error("Failed receive machine's response");
                return MachineConstants.BASE_UNIT_FAILURE;
            } else {
                int returnCode = byteArray[0];
                if (returnCode == MachineConstants.BASE_UNIT_COMPLETED) {
                    logger.info("Unit execution completed successfully!");
                } else {
                    logger.error("Error occurred while executing unit.");
                }
                return returnCode;
            }
        } catch (IOException e) {
            logger.error("Error during sendBaseUnitRequest: " + e.getMessage());
            return MachineConstants.BASE_UNIT_FAILURE;
        } finally {
            if (socket!=null)
                try {
                    socket.close();
                } catch (IOException e1) {
                    // do nothing
                }
        }
    }
}