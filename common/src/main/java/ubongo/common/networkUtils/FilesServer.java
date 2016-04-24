package ubongo.common.networkUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.constants.SystemConstants;
import ubongo.common.exceptions.NetworkException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;

public class FilesServer {

    private static Logger logger = LogManager.getLogger(FilesServer.class);
    private Socket socket;
    private int connectionPort;
    private String baseDir;

    public FilesServer(int connectionPort, String baseDir) {
        this.connectionPort = connectionPort;
        this.baseDir = baseDir;
    }


    /**
     * Calls start and transferFileToMachine, and handles exceptions by re-try.
     */
    public void manageFileTransfer(String relativeDirPath) {
        int success = 0;
        int retries = 0;
        while ((success == 0) && (retries < SystemConstants.NETWORK_RETRIES)){
            retries++;
            try {
                start();
                success = 1;
            } catch (IOException e) {
                success = 0;
                try {
                    Thread.sleep(SystemConstants.SLEEP_BETWEEN_NETWORK_RETRIES);
                } catch (InterruptedException e1) {
                    // do nothing
                }
            }
        }

        if (success==1){
            success=0;
            retries=0;
            while ((success == 0) && (retries < SystemConstants.NETWORK_RETRIES)){
                retries++;
                try {
                    transferFileToMachine(relativeDirPath);
                    success = 1;
                } catch (NetworkException e) {
                    success = 0;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        // do nothing
                    }
                }
            }
        }

        if (success==0){
            logger.error("Failure after " + retries + " retries.");
        }
    }

    private void start() throws IOException{
        try {
            logger.info("FilesServer listening on port = [" + connectionPort + "]");
            ServerSocket serverSocket = new ServerSocket(connectionPort);
            socket = serverSocket.accept();
            logger.info("Accepted connection : " + socket);
        } catch (IOException e) {
            String errorMsg = "Failed to create socket.\nDetails: "+ e.getMessage();
            logger.error(errorMsg);
            socket = null;
            throw e;
        }
    }

    /**
     * Sends files from relativeDirPath (absolute path is baseDir/relativeDirPath) to the chosen machine using socket.
     */
    private void transferFileToMachine(String relativeDirPath) throws NetworkException {
        try {
            File filesToSend[] = this.getFilesInDir(relativeDirPath);
            for (File transferFile : filesToSend) {
                String fileName = transferFile.getName();
                long fileSize = transferFile.length();

                byte[] byteArray = new byte[(int) fileSize];
                BufferedInputStream bufferedInputStream =
                        new BufferedInputStream(new FileInputStream(transferFile));
                bufferedInputStream.read(byteArray, 0, byteArray.length);
                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(outputStream));
                logger.debug("Sending file [" + fileName + "]...");

                dataOutputStream.writeUTF(fileName.substring(fileName.lastIndexOf("\\") + 1));
                dataOutputStream.writeLong(fileSize);
                dataOutputStream.flush();
                outputStream.write(byteArray, 0, byteArray.length);
                outputStream.flush();
                logger.info("File transfer completed");
            }
        } catch (IOException e) {
            String errorMsg = "Failed to send files.\nDetails: "+ e.getMessage();
            logger.error(errorMsg);
            throw new NetworkException(errorMsg);
        } finally {
            closeSocket();
        }
    }

    /**
     * @return list of files in "relativeDirPath".
     */
    public File[] getFilesInDir(String relativeDirPath) {
        File folder = new File(baseDir, relativeDirPath);
        File[] ret;
        try {
            ret = folder.listFiles();
        } catch (NullPointerException e) {
            ret = new File[0];
        }
        return ret;
    }

    private void closeSocket(){
        try {
            this.socket.close();
        } catch (IOException e){
            String errorMsg = "Failed to close socket.\nLogs: "+ e.getMessage();
            logger.error(errorMsg);
        } catch (Exception e){ // TODO remove - tmp for tests
        String errorMsg = "Failed to close socket.\nLogs: "+ e.getMessage();
        logger.error(errorMsg);
        }
    }

}
