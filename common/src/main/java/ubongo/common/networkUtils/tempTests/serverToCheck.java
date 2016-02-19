package ubongo.common.networkUtils.tempTests;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class serverToCheck {

    public static void main(String [ ] args) throws IOException
    {
        transferFileToMachine("testDir");
    }


    /**
     * Sends files from "dirPath" to the chosen machine using socket.
     */
    public static void transferFileToMachine(String dirPath) throws IOException {
        ServerSocket serverSocket = new ServerSocket(65123);
        Socket socket = serverSocket.accept();
        File filesToSend[] = getFilesInDir(dirPath);
        for (File transferFile : filesToSend){
            String fileName = transferFile.getName();
            long fileSize = transferFile.length();

            byte[] bytearray = new byte[(int) fileSize];
            FileInputStream fin = new FileInputStream(transferFile);
            BufferedInputStream bin = new BufferedInputStream(fin);
            bin.read(bytearray, 0, bytearray.length);
            OutputStream os = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
            System.out.println("Sending file [" + fileName + "]...");

            dos.writeUTF(fileName);
            dos.writeLong(fileSize);
            dos.flush();
            os.write(bytearray, 0, bytearray.length);
            os.flush();
            System.out.println("File transfer completed");
        }
        socket.close();
        //socket.close();
    }


    /**
     * Returns list of files in "dirPath".
     */
    public static File[] getFilesInDir(String dirPath){
        File folder = new File(dirPath);
        return folder.listFiles();
    }

}
