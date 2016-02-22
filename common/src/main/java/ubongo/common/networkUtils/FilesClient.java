package ubongo.common.networkUtils;


import ubongo.common.exceptions.NetworkException;
import ubongo.common.log.Logger;
import ubongo.common.log.LoggerManager;

import java.io.*;
import java.net.Socket;

public class FilesClient {

    private static Logger logger = LoggerManager.getLogger(FilesClient.class);
    private Socket socket;
    private String baseDir;

    public FilesClient(String sourceMachine, int connectionPort, String baseDir) throws NetworkException{
        this.baseDir = baseDir;
        try {
            logger.info("Connecting to socket on machine = [" + sourceMachine + "], on port = [" + connectionPort + "]");
            this.socket = new Socket(sourceMachine, connectionPort);
            logger.debug("Socket was initialized successfully");
        } catch (IOException e) {
            String errorMsg = "Failed to create socket.\nDetails: " + e.getMessage();
            logger.error(errorMsg);
            this.socket = null;
            throw new NetworkException(errorMsg);
        }
    }

    /**
     * Receives files using socket.
     * Used for receiving files from the main files server to the machines, or from a machine to the files server.
     * @return the number of received files.
     */
    public int getFilesFromServer(int numFiles) throws IOException{
        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        for (int i = 0; i < numFiles; i++){
            try {
                String filename = dataInputStream.readUTF();
                long fileSize = dataInputStream.readLong();
                byte[] byteArray = new byte[(int) fileSize];
                BufferedOutputStream bufferedOutputStream =
                        new BufferedOutputStream(new FileOutputStream(new File(baseDir, filename)));
                logger.debug("Transfer of file [" + filename + "] started...");
                int currentTotalBytesRead = 0;
                int bytesRead;
                do {
                    bytesRead = dataInputStream.read(byteArray, currentTotalBytesRead,
                            (byteArray.length - currentTotalBytesRead));
                    if (bytesRead >= 0) {
                        currentTotalBytesRead += bytesRead;
                    }
                } while (fileSize > 0 && bytesRead > 0);
                bufferedOutputStream.write(byteArray, 0, currentTotalBytesRead);
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
                logger.info("Received file [" + filename + "]");
            } catch (EOFException e) {
                logger.error("Not enough files were sent! Received [" + (i + 1) + "] instead of [" + numFiles + "]");
                return i;
            } catch (IOException e) {
                String errorMsg = "Unexpected error. \nDetails: "+ e.getMessage();
                logger.error(errorMsg);
                return i;
            }
        }
        closeSocket();
        return numFiles;
    }

    public void closeSocket(){
        try {
            socket.close();
        } catch (IOException e){
            String errorMsg = "Failed to close socket.\nDetails: " + e.getMessage();
            logger.error(errorMsg);
        }
    }

}
