package edu.tau.eng.neuroscience.mri.common.networkUtils;


import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;

import java.io.*;
import java.net.Socket;

public class FilesClient {

    private static Logger logger = LoggerManager.getLogger(FilesClient.class);
    private Socket socket;

    public FilesClient(String sourceMachine, int connectionPort) {
        try {
            logger.info("Connecting to socket on machine = [" + sourceMachine+"], on port = ["+connectionPort+"]");
            this.socket = new Socket(sourceMachine, connectionPort);
            logger.debug("Socket was initialized successfully");
        } catch (IOException e) {
            String errorMsg = "Failed to create socket.\nLogs: "+ e.getMessage();
            logger.error(errorMsg);
            this.socket = null; // TODO Throw new exception instead
        }
    }

    /**
     * Receives files using socket.
     * Used for receiving files from the main files server to the machines, or from a machine to the files server.
     * @return the number of received files.
     */

    public int getFilesFromServer(int numberOfFiles) throws IOException{
        int bytesRead;
        int currentTot;

        DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        for (int i=0; i<numberOfFiles; i++){
            try {
                String filename = dis.readUTF();
                long fileSize = dis.readLong();
                byte[] bytearray = new byte[(int) fileSize];
                FileOutputStream fos = new FileOutputStream(filename);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                logger.debug("Transfer of file [" +filename+"] started...");
                bytesRead = dis.read(bytearray, 0, bytearray.length);
                currentTot =0;
                if (bytesRead >= 0) currentTot += bytesRead;
                fileSize = fileSize-bytesRead;
                while ((fileSize>0) && (bytesRead > -1)) {
                    bytesRead = dis.read(bytearray, currentTot, (bytearray.length - currentTot));
                    if (bytesRead >= 0) currentTot += bytesRead;
                }
                bos.write(bytearray, 0, currentTot);
                bos.flush();
                bos.close();
                logger.info("Received file ["+filename+"]");
            } catch (EOFException e) {
                logger.error("Not enough files were sent! Received [" + (i+1) + "] instead of [" + numberOfFiles+"]");
                return i;
            } catch (IOException e) {
                String errorMsg = "Unexpected error. \nLogs: "+ e.getMessage();
                logger.error(errorMsg);
                return i;
            }
        }
        return numberOfFiles;
    }

    public void closeSocket(){
        try {
            this.socket.close();
        } catch (IOException e){
            String errorMsg = "Failed to create socket.\nLogs: "+ e.getMessage();
            logger.error(errorMsg);
        }
    }

}
