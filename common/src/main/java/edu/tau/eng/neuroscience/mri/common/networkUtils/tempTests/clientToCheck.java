package edu.tau.eng.neuroscience.mri.common.networkUtils.tempTests;

import java.io.*;
import java.net.Socket;


public class clientToCheck {

    public static void main(String [ ] args) throws IOException
    {
        int num = getFilesFromServer(2);
        System.out.println("res = ["+num+"]");

    }


    public static int getFilesFromServer(int numberOfFiles) throws IOException{
        int bytesRead;
        int currentTot;
        Socket socket = new Socket("nova.cs.tau.ac.il", 57982);
        DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        for (int i=0; i<numberOfFiles; i++){
            try {
                String filename = dis.readUTF();
                long fileSize = dis.readLong();
                byte[] bytearray = new byte[(int) fileSize]; /* TODO need to verify that this is safe*/
                FileOutputStream fos = new FileOutputStream(filename);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                System.out.println("Transfer started...");
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
                System.out.println("Received file [" + filename + "]");
            } catch (EOFException e) {
                System.out.println("Not enough files were sent! Received [" + (i + 1) + "] instead of [" + numberOfFiles + "]");
                return i;
            } catch (IOException e) {
                String errorMsg = "Unexpected error. \nLogs: "+ e.getMessage();
                System.out.println(errorMsg);
                return i;
            }
        }
        socket.close();
        return numberOfFiles;
    }
}
