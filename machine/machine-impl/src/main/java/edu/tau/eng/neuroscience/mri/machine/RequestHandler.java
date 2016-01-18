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

    public RequestHandler(Socket socket) {
        super("RequestHandler");
        this.socket = socket;
    }

    public void run() {
        logger.debug("run() - Start");
        try {

            DataInputStream dataInputStream =
                    new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            byte[] messageLength = new byte[1];
            int bytesReadForLength = dataInputStream.read(messageLength, 0, 1);

            if (bytesReadForLength != 1){
                throw new IOException("Request length - received with error");
            }

            int requestLength = messageLength[0];

            logger.debug("Incoming request length = [" + requestLength + "]");
            byte[] byteArray = new byte[requestLength];
            int bytesRead = dataInputStream.read(byteArray, 0, byteArray.length);
            int totalRead = 0;
            if (bytesRead >= 0) {
                totalRead += bytesRead;
            }
            logger.debug("total read bytes from stream = [" + totalRead + "]");
            while (bytesRead > -1 && totalRead < requestLength) {
                bytesRead = dataInputStream.read(byteArray, totalRead, (byteArray.length - totalRead));
                if (bytesRead >= 0) {
                    totalRead += bytesRead;
                }
                logger.debug("total read bytes from stream = [" + totalRead + "]");
            }

            byte[] totalRequestBytesArray = Arrays.copyOf(byteArray, totalRead);
            logger.debug("After totalRequestBytesArray");

            String[] parsedRequest = (new String(totalRequestBytesArray)).split(" ");
            logger.info("Parsed request = [" + Integer.parseInt(parsedRequest[0])+" "+Integer.parseInt(parsedRequest[1]) + " "+Integer.parseInt(parsedRequest[2])+"]");
            int idInput = Integer.parseInt((parsedRequest[0]));
            logger.debug("idInput = " + idInput + "]");
            switch (idInput){
                case MachineConstants.RECEIVE_FILES_REQUEST:
                    logger.info("Receive file request. Calling handleReceiveFilesRequest");
                    handleReceiveFilesRequest(parsedRequest);
                    break;
                case MachineConstants.BASE_UNIT_REQUEST:
                    handleBaseUnitRequest(parsedRequest);
                    break;
                default:
                    logger.error("Undefined input ID : " + idInput);
            }

            logger.info("Closing request socket.");
            socket.close();

        } catch (IOException e) {
            logger.error("Failed handling request : " + e.getMessage());
        }
    }

    private void handleReceiveFilesRequest(String[] parsedRequest){
        int connectionPort = Integer.parseInt(parsedRequest[1]);
        int requestedNumForReceive = Integer.parseInt(parsedRequest[2]);
        logger.info("handleReceiveFilesRequest - start. Connection port = ["+connectionPort+"] requested number of files = ["+requestedNumForReceive+"] from server = ["+MachineConstants.FILESERVER+"]" );
        FilesClient filesClient = new FilesClient(MachineConstants.FILESERVER, connectionPort);
        try {
            int receivedFiles = filesClient.getFilesFromServer(requestedNumForReceive);
            OutputStream outputStream = socket.getOutputStream();
            byte completionStatus[] = new byte[1];

            if (receivedFiles<requestedNumForReceive){
                completionStatus[0] = MachineConstants.RECEIVE_ERROR;
                logger.error("Received [" + receivedFiles + "] instead of ["+requestedNumForReceive+"]");
            } else {
                logger.info("Received all files [" + receivedFiles + "]");
                completionStatus[0] = MachineConstants.RECEIVED_ALL_FILES;
            }
            outputStream.write(completionStatus, 0, completionStatus.length);
        } catch (IOException e) {
            logger.error("Failed receiving files from server " + e.getMessage());
        }
    }

    public void handleBaseUnitRequest(String[] parsedRequest){
        // TODO parse request. rebuild unit object and call machine controller impl run method
    }
}