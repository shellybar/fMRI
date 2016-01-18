package edu.tau.eng.neuroscience.mri.common.networkUtils.tempTests;

import java.io.*;
import java.net.Socket;


public class clientToCheck {

    public static void main(String[] args) throws IOException {
        int num = getFilesFromServer(2);
        System.out.println("res = ["+num+"]");
    }

    public static int getFilesFromServer(int numberOfFiles) throws IOException {
        int bytesRead;
        int currentTotalBytesRead;
        Socket socket = new Socket("nova.cs.tau.ac.il", 57982);
        DataInputStream dataInputStream =
                new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        for (int i = 0; i < numberOfFiles; i++){
            try {
                String filename = dataInputStream.readUTF();
                long fileSize = dataInputStream.readLong();
                byte[] byteArray = new byte[(int) fileSize]; // TODO need to verify that this is safe
                FileOutputStream fileOutputStream = new FileOutputStream(filename);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                System.out.println("Transfer started...");
                bytesRead = dataInputStream.read(byteArray, 0, byteArray.length);
                currentTotalBytesRead =0;
                if (bytesRead >= 0) {
                    currentTotalBytesRead += bytesRead;
                }
                fileSize = fileSize-bytesRead;
                while (fileSize > 0 && bytesRead > -1) {
                    bytesRead = dataInputStream.read(byteArray, currentTotalBytesRead, (byteArray.length - currentTotalBytesRead));
                    if (bytesRead >= 0) {
                        currentTotalBytesRead += bytesRead;
                    }
                }
                bufferedOutputStream.write(byteArray, 0, currentTotalBytesRead);
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
                System.out.println("Received file [" + filename + "]");
            } catch (EOFException e) {
                System.out.println("Not enough files were sent! Received [" + (i + 1) + "] instead of [" + numberOfFiles + "]");
                return i;
            } catch (IOException e) {
                String errorMsg = "Unexpected error. \nLogs: " + e.getMessage();
                System.out.println(errorMsg);
                return i;
            }
        }
        socket.close();
        return numberOfFiles;
    }
}
