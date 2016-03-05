package ubongo.dispatcher;


import ubongo.common.constants.MachineConstants;
import ubongo.common.constants.SystemConstants;
import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.MachineStatistics;
import ubongo.common.datatypes.Task;
import ubongo.common.log.Logger;
import ubongo.common.log.LoggerManager;

import java.io.*;
import java.net.Socket;

public enum ExecutionProxy {

    INSTANCE; // This is a singleton

    public static ExecutionProxy getInstance() {
        return INSTANCE;
    }

    private static Logger logger = LoggerManager.getLogger(ExecutionProxy.class);
    private String inputDirPath;
    private QueueManager queueManager;

    /**
     * @param task to execute
     * @param queueManager to send the task back after execution
     */
    public void execute(Task task, QueueManager queueManager) {
        this.queueManager = queueManager;
        int returnCode = excecuteTaskOnTheMachine(task);
        /* TODO continue - check return code etc. */



        // TODO move this part of the code to the method called by the client after execution (it's here only to test the dispatcher)
        //task.setStatus(TaskStatus.COMPLETED);
        //queueManager.updateTaskAfterExecution(task);
    }

    public MachineStatistics getStatistics(Machine machine) {
        return new MachineStatistics(null); // TODO
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