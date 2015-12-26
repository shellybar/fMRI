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
        try {
            byte[] bytearray = new byte[MachineConstants.MAX_INPUT_BYTES];
            DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            int bytesRead = dis.read(bytearray, 0, bytearray.length);
            int totalRead = 0;
            if (bytesRead >= 0) totalRead += bytesRead;
            while (bytesRead > -1) {
                bytesRead = dis.read(bytearray, totalRead, (bytearray.length - totalRead));
                if (bytesRead >= 0) totalRead += bytesRead;
            }

            byte[] totalRequestBytesArray = Arrays.copyOf(bytearray, totalRead);
            MachineControllerImpl mc = new MachineControllerImpl();
            String[] parsedRequest = (new String(totalRequestBytesArray)).split(" ");
            int idInput = Integer.parseInt((parsedRequest[0]));
            switch (idInput){
                case MachineConstants.RECEIVE_FILES_REQUEST:
                    handleReceiveFilesRequest(parsedRequest);
                    break;
                case MachineConstants.BASE_UNIT_REQUEST:
                    handleBaseUnitRequest(parsedRequest);
                    break;
                default:
                    logger.error("Undefined input ID : " + idInput);
            }

            socket.close();

        } catch (IOException e) {
            logger.error("Failed handling request : " + e.getMessage());
        }
    }

    private void handleReceiveFilesRequest(String[] parsedRequest){
        int connectionPort = Integer.parseInt(parsedRequest[1]);
        int requestedNumForReceive = Integer.parseInt(parsedRequest[2]);
        FilesClient fc = new FilesClient(MachineConstants.FILESERVER, connectionPort);
        try {
            int receivedFiles = fc.getFilesFromServer(requestedNumForReceive);
            OutputStream os = socket.getOutputStream();
            byte completionStatus[] = new byte[1];


            if (receivedFiles<requestedNumForReceive){
                completionStatus[0] = MachineConstants.RECEIVE_ERROR;
                logger.error("Received [" + receivedFiles + "] instead of ["+requestedNumForReceive+"]");
            } else {
                logger.info("Received all files[" + receivedFiles + "]");
                completionStatus[0] = MachineConstants.RECEIVED_ALL_FILES;
            }
            os.write(completionStatus, 0, completionStatus.length);

        } catch (IOException e) {
            logger.error("Failed receiving files from server " + e.getMessage());
        }
    }

    public void handleBaseUnitRequest(String[] parsedRequest){
        /* TODO parse request. rebuiled unit object and call machine controller impl run method*/
    }

}