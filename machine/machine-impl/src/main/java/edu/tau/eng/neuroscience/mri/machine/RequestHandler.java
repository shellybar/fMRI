package edu.tau.eng.neuroscience.mri.machine;


import edu.tau.eng.neuroscience.mri.common.constants.MachineConstants;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.common.networkUtils.FilesClient;

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

            // TODO please create an object or three variables instead of parsedRequest - it's hard to understand what each element of this array means
            String[] parsedRequest = (new String(totalRequestBytesArray)).split(" ");
            int idInput = Integer.parseInt((parsedRequest[0]));
            int connectionPort = Integer.parseInt(parsedRequest[1]);
            int requestedNumForReceive = Integer.parseInt(parsedRequest[2]);
            logger.info("Parsed request = [" + idInput + " " + connectionPort + " " + requestedNumForReceive + "]");
            logger.debug("idInput = " + idInput + "]");
            if (idInput == MachineConstants.RECEIVE_FILES_REQUEST) {
                logger.info("Received file request. Calling handleReceiveFilesRequest");
                handleReceiveFilesRequest(connectionPort, requestedNumForReceive);
            } else if (idInput == MachineConstants.BASE_UNIT_REQUEST) {
                handleBaseUnitRequest(connectionPort, requestedNumForReceive);
            } else {
                logger.error("Undefined input ID: " + idInput);
            }
            logger.info("Closing request socket.");
            socket.close();
        } catch (IOException e) {
            logger.error("Failed handling request: " + e.getMessage());
        }
    }

    private void handleReceiveFilesRequest(int connectionPort, int requestedNumForReceive){
        logger.info("handleReceiveFilesRequest - start. Connection port = [" + connectionPort + "] " +
                "requested number of files = [" + requestedNumForReceive + "] " +
                "from server = [" + serverAddress + "]" );
        FilesClient filesClient = new FilesClient(serverAddress, connectionPort, baseDir);
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

    public void handleBaseUnitRequest(int connectionPort, int requestedNumForReceive){
        // TODO parse request. rebuild unit object and call machine controller impl run method
    }
}