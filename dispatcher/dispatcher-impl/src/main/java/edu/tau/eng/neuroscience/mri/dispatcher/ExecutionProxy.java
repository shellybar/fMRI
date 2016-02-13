package edu.tau.eng.neuroscience.mri.dispatcher;


import edu.tau.eng.neuroscience.mri.common.constants.MachineConstants;
import edu.tau.eng.neuroscience.mri.common.datatypes.Machine;
import edu.tau.eng.neuroscience.mri.common.datatypes.MachineStatistics;
import edu.tau.eng.neuroscience.mri.common.datatypes.Task;
import edu.tau.eng.neuroscience.mri.common.datatypes.TaskStatus;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.common.networkUtils.FilesServer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
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
        //int returnCode = sendInputFiles(task);
        /* TODO continue - check return code etc. */

        // TODO move this part of the code to the method called by the client after execution (it's here only to test the dispatcher)
        task.setStatus(TaskStatus.COMPLETED); //TODO or Failed
        queueManager.updateTaskAfterExecution(task);
    }

    public MachineStatistics getStatistics(Machine machine) {
        return new MachineStatistics(); // TODO
    }

    public int sendInputFiles(Task task) {

        logger.info("Starting to send input files. Task id = ["+task.getId()+"]");
        int connectionPort = task.getId(); /* TODO use an external adapter to convert task_id to port */
        server = new FilesServer(connectionPort);
        inputDirPath = task.getUnit().getInputPath();
        logger.info("Input path: " + inputDirPath);
        int numberOfInputFiles = server.getFilesInDir(inputDirPath).length;
        logger.info("Number of input files: " + numberOfInputFiles);

        // Start the process for sending files on a new thread, and start the client on the designated machine.

        Runnable myRunnableServer = () -> server.transferFileToMachine(inputDirPath);
        Thread thread = new Thread(myRunnableServer);
        thread.start();

        // Start the client on the designated machine and wait for response.
        int returnCode = sendGetFilesRequest(task, connectionPort, numberOfInputFiles);
        return returnCode;
    }

    /**
     * Sends remote request to the machine using socket
     *
     * RECEIVE_FILES_REQUEST format: RECEIVE_FILES_REQUEST CONNECTION_PORT NUM_OF_FILES
     * BASE_UNIT_REQUEST format:  TODO
     *
     */
    private int sendGetFilesRequest(Task task, int connectionPort, int numberOfInputFiles) {
        String request = MachineConstants.RECEIVE_FILES_REQUEST + " " + connectionPort + " " + numberOfInputFiles;
        char requestLenInChar = (char) request.length();
        String requestWithLength = requestLenInChar + request;
        logger.info("Sending request: " + requestWithLength);
        Socket socket = null;
        try {
            socket = new Socket(task.getMachine().getAddress(), MachineConstants.MACHINE_SERVER_PORT); /* TODO : add sleep and retries! because this port is used only for request, it could be in use for a short time*/
            logger.info("Initialized socket to designated machine: ip=[" + task.getMachine().getAddress()
                    + "], port=[" + MachineConstants.MACHINE_SERVER_PORT + "]");
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
                if (returnCode == MachineConstants.RECEIVED_ALL_FILES)
                    logger.info("All files received successfully!");
                else
                    logger.error("Error occur while sending files.");
                return returnCode;
            }
        } catch (IOException e) {
            logger.error("Error during sendGetFilesRequest: " + e.getMessage());
            return MachineConstants.RECEIVE_ERROR;
        } finally {
            try {
                if (socket != null || socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                // do nothing
            }
        }
    }
}
