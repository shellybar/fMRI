package edu.tau.eng.neuroscience.mri.common.networkUtils;

import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;

public class FilesServer {

    private static Logger logger = LoggerManager.getLogger(FilesServer.class);
    private Socket socket;
    private int connectionPort;

    public FilesServer(int connectionPort) {
        this.connectionPort = connectionPort;
    }

    private void start() {
        try {
            logger.info("FilesServer listening on port = ["+connectionPort+"]");
            ServerSocket serverSocket = new ServerSocket(connectionPort);
            socket = serverSocket.accept();
            logger.info("Accepted connection : " + socket);
        } catch (IOException e) {
            String errorMsg = "Failed to create socket.\nLogs: "+ e.getMessage();
            logger.error(errorMsg);
            socket = null; // TODO throw new exception instead
        }
    }

    /**
     * Sends files from "dirPath" to the chosen machine using socket.
     */
    public void transferFileToMachine(String dirPath) {
        start(); // TODO accept blocks and then the log is not written
        try {
            File filesToSend[] = this.getFilesInDir(dirPath);
            for (File transferFile : filesToSend) {
                String fileName = transferFile.getName();
                long fileSize = transferFile.length();

                byte[] byteArray = new byte[(int) fileSize];
                FileInputStream fileInputStream = new FileInputStream(transferFile);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                bufferedInputStream.read(byteArray, 0, byteArray.length);
                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(outputStream));
                logger.debug("Sending file [" + fileName + "]...");

                dataOutputStream.writeUTF(fileName);
                dataOutputStream.writeLong(fileSize);
                dataOutputStream.flush();
                outputStream.write(byteArray, 0, byteArray.length);
                outputStream.flush();
                logger.info("File transfer completed");
            }
        } catch (IOException e) {
            //TODO
        } finally {
            closeSocket();
        }
    }


    /**
     * @return list of files in "dirPath".
     */
    public File[] getFilesInDir(String dirPath) {
        File folder = new File(dirPath);
        File[] ret;
        try {
            ret = folder.listFiles();
        } catch (NullPointerException e) {
            ret = null;
        }
        return (ret == null) ? new File[0] : ret;
    }

    private void closeSocket(){
        try {
            this.socket.close();
        } catch (IOException e){
            String errorMsg = "Failed to create socket.\nLogs: "+ e.getMessage();
            logger.error(errorMsg);
        }
    }

}
