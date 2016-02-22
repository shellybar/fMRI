package ubongo.machine;


import ubongo.common.constants.MachineConstants;
import ubongo.common.datatypes.Task;
import ubongo.common.exceptions.NetworkException;
import ubongo.common.log.Logger;
import ubongo.common.log.LoggerManager;
import ubongo.common.networkUtils.FilesClient;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

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
    private Socket socket = null;
    private String serverAddress; // Address of the program server
    private String baseDir; // The root directory where the files should be stored

    public RequestHandler(Socket socket, String serverAddress, String baseDir) {
        super("RequestHandler");
        this.socket = socket;
        this.serverAddress = serverAddress;
        this.baseDir = baseDir;
    }

    @Override
    public void run() {
        logger.debug("run() - Start");
        try {
            DataInputStream dataInputStream =
                    new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            byte[] messageLength = new byte[1];
            if (dataInputStream.read(messageLength, 0, 1) != 1) {
                throw new IOException("Request length - received with error");
            }

            int requestLength = messageLength[0];

            logger.debug("Incoming request length = [" + requestLength + "]");
            byte[] byteArray = new byte[requestLength];
            int bytesRead;
            int totalRead = 0;
            do {
                bytesRead = dataInputStream.read(byteArray, totalRead, (byteArray.length - totalRead));
                if (bytesRead >= 0) {
                    totalRead += bytesRead;
                }
                logger.debug("Current total read bytes from stream = [" + totalRead + "]");
            } while (bytesRead > -1 && totalRead < requestLength);

            byte[] totalRequestBytesArray = Arrays.copyOf(byteArray, totalRead);

            String[] parsedRequest = (new String(totalRequestBytesArray)).split(" ");
            int idInput = Integer.parseInt((parsedRequest[0]));
            int connectionPort = Integer.parseInt(parsedRequest[1]);

            logger.info("Parsed request = [" + idInput + " " + connectionPort +"]");
            if (idInput == MachineConstants.RECEIVE_FILES_REQUEST) {
                int requestedNumForReceive = Integer.parseInt(parsedRequest[2]);
                logger.info("Parsed request = [" + idInput + " " + connectionPort + " " + requestedNumForReceive + "]");
                logger.info("Received file request. Calling handleReceiveFilesRequest");
                String inputFilesDir = this.baseDir + File.separator  + connectionPort + "_in";
                handleReceiveFilesRequest(connectionPort, requestedNumForReceive, inputFilesDir);
            } else if (idInput == MachineConstants.BASE_UNIT_REQUEST) {
                String outputFilesDir = this.baseDir + File.separator  + connectionPort + "_out";
                ObjectInputStream objectOutputStream = new ObjectInputStream(dataInputStream);
                Task task = null;
                try {
                    task = (Task) objectOutputStream.readObject();
                    handleBaseUnitRequest(connectionPort, outputFilesDir, task);
                } catch (ClassNotFoundException e) {
                    logger.error("Error while receiving task object: " + e.getMessage());
                }
            } else {
                logger.error("Undefined input ID: " + idInput);
            }
            logger.info("Closing request socket.");
            socket.close();
        } catch (IOException e) {
            logger.error("Failed handling request: " + e.getMessage());
        }
    }

    private void handleReceiveFilesRequest(int connectionPort, int requestedNumForReceive, String inputFilesDir){
        logger.info("handleReceiveFilesRequest - start. Connection port = [" + connectionPort + "] " +
                "requested number of files = [" + requestedNumForReceive + "] " +
                "from server = [" + serverAddress + "]" );

        File inputDir = new File(inputFilesDir);
        if (inputDir.exists()) {
            logger.error("input Dir already exists..."); // TODO handle this !
            return;
        }
        boolean result = false;
        try{
            inputDir.mkdir();
            result = true;
        }
        catch(SecurityException se){
            logger.error("Failed to create input Dir " + inputFilesDir); // TODO handle this !
            return;
        }
        if(!result) {
            logger.error("Failed to create input Dir " + inputFilesDir); // TODO handle this !
            return;
        }


        FilesClient filesClient = null;
        try {
            filesClient = new FilesClient(serverAddress, connectionPort, inputFilesDir);
        } catch (NetworkException e) {
            return;
        }
        try {
            int receivedFiles = filesClient.getFilesFromServer(requestedNumForReceive);
            OutputStream outputStream = socket.getOutputStream();
            byte completionStatus[] = new byte[1];

            if (receivedFiles < requestedNumForReceive){
                completionStatus[0] = MachineConstants.RECEIVE_ERROR;
                logger.error("Received [" + receivedFiles + "] instead of [" + requestedNumForReceive + "]");
            } else {
                logger.info("Received all files [" + receivedFiles + "]");
                completionStatus[0] = MachineConstants.RECEIVED_ALL_FILES;
            }
            outputStream.write(completionStatus, 0, completionStatus.length);
        } catch (IOException e) {
            logger.error("Failed receiving files from server " + e.getMessage());
        }
    }

    public void handleBaseUnitRequest(int connectionPort, String outputFilesDir, Task task){
        logger.info("handleBaseUnitRequest - start. Connection port = [" + connectionPort + "] " +
                "from server = [" + serverAddress + ". task ID = " +task.getId() +"]" );

        System.out.println("task ID = "+task.getId()); // TODO remove
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


//        FilesClient filesClient = null;
//        try {
//            filesClient = new FilesClient(serverAddress, connectionPort, outputFilesDir);
//        } catch (NetworkException e) {
//            return;
//        }
//        try {
//            int receivedFiles = filesClient.getFilesFromServer(requestedNumForReceive);
//            OutputStream outputStream = socket.getOutputStream();
//            byte completionStatus[] = new byte[1];
//
//            if (receivedFiles < requestedNumForReceive){
//                completionStatus[0] = MachineConstants.RECEIVE_ERROR;
//                logger.error("Received [" + receivedFiles + "] instead of [" + requestedNumForReceive + "]");
//            } else {
//                logger.info("Received all files [" + receivedFiles + "]");
//                completionStatus[0] = MachineConstants.RECEIVED_ALL_FILES;
//            }
//            outputStream.write(completionStatus, 0, completionStatus.length);
//        } catch (IOException e) {
//            logger.error("Failed receiving files from server " + e.getMessage());
//        }
    }

}