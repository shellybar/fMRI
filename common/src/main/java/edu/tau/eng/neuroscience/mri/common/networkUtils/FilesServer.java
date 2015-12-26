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

    public FilesServer(int connectionPort) {
        try {
            ServerSocket serverSocket = new ServerSocket(connectionPort);
            socket = serverSocket.accept();
            logger.info("Accepted connection : " + socket);
        } catch (IOException e) {
            String errorMsg = "Failed to create socket.\nLogs: "+ e.getMessage();
            logger.error(errorMsg);
            this.socket = null; // TODO th new exception instead
        }
    }

    /**
     * Sends files from "dirPath" to the chosen machine using socket.
     */
    public void transferFileToMachine(String dirPath) throws IOException {
        File filesToSend[] = this.getFilesInDir(dirPath);
        for (File transferFile : filesToSend){
            String fileName = transferFile.getName();
            long fileSize = transferFile.length();

            byte[] bytearray = new byte[(int) fileSize];
            FileInputStream fin = new FileInputStream(transferFile);
            BufferedInputStream bin = new BufferedInputStream(fin);
            bin.read(bytearray, 0, bytearray.length);
            OutputStream os = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
            logger.debug("Sending file [" + fileName + "]...");

            dos.writeUTF(fileName);
            dos.writeLong(fileSize);
            dos.flush();
            os.write(bytearray, 0, bytearray.length);
            os.flush();
            logger.info("File transfer completed");
        }
        //socket.close();
    }


    /**
     * @return list of files in "dirPath".
     */
    public File[] getFilesInDir(String dirPath){
        File folder = new File(dirPath);
        return folder.listFiles();
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
