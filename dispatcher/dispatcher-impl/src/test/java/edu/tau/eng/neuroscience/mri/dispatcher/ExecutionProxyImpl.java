package edu.tau.eng.neuroscience.mri.dispatcher;


import edu.tau.eng.neuroscience.mri.common.constants.MachineConstants;
import edu.tau.eng.neuroscience.mri.common.datatypes.Machine;
import edu.tau.eng.neuroscience.mri.common.datatypes.MachineStatistics;
import edu.tau.eng.neuroscience.mri.common.datatypes.Task;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.common.networkUtils.FilesServer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ExecutionProxyImpl implements ExecutionProxy {

    private static Logger logger = LoggerManager.getLogger(ExecutionProxyImpl.class);
    private Task task;
    private FilesServer server;
    private String inputDirPath;

    public ExecutionProxyImpl(Task task) {
        this.task = task;
    }

    @Override
    public void execute(Task task) {
        int returnCode =  this.sendInputFiles();
        /* TODO continue - check return code etc. */

    }

    @Override
    public MachineStatistics getStatistics(Machine machine) {
        return null;
    }

    public int sendInputFiles() {
        int connectionPort = this.task.getTaskId(); /* TODO use an external adapter to convert task_id to port */
        server = new FilesServer(connectionPort);
        inputDirPath = this.task.getUnit().getInputPath();
        int numberOfInputFiles = server.getFilesInDir(inputDirPath).length;

        // Start the process for sending files on a new thread, and start the client on the designated machine.

        Runnable myRunnableServer = () -> server.getFilesInDir(inputDirPath);
        Thread thread = new Thread(myRunnableServer);
        thread.start();

        // Start the client on the designated machine and wait for response.
        int returnCode = sendGetFilesRequest(connectionPort, numberOfInputFiles);
        server.closeSocket();
        return returnCode;
    }

    /**
     * Sends remote request to the machine using HTTP
     *
     * RECEIVE_FILES_REQUEST format: RECEIVE_FILES_REQUEST CONNECTION_PORT NUM_OF_FILES
     * BASE_UNIT_REQUEST format:  TODO
     *
     */
    private int sendGetFilesRequest(int connectionPort, int numberOfInputFiles) {
        String request = MachineConstants.RECEIVE_FILES_REQUEST + " " + connectionPort + " " + numberOfInputFiles;
        try {
            Socket socket = new Socket(task.getMachine().toString(), connectionPort);
            OutputStream os = socket.getOutputStream();
            os.write(request.getBytes());
            os.flush();

            // Now wait for response

            byte[] bytearray = new byte[1];
            DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            int bytesRead = dis.read(bytearray, 0, bytearray.length);
            if (bytesRead<0){
                logger.error("Failed receive machine's response");
                return MachineConstants.RECEIVE_ERROR;
            } else {
                int returnCode = bytearray[0];
                if (returnCode == MachineConstants.RECEIVED_ALL_FILES)
                    logger.info("All files received successfully!");
                else
                    logger.error("Error occur while sending files.");

                return returnCode;
            }
        } catch (IOException e) {
            logger.error("Error during sendGetFilesRequest: " + e.getMessage());
            return MachineConstants.RECEIVE_ERROR;
        }


    }
}
