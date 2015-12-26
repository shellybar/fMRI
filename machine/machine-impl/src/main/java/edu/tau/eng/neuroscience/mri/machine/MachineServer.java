package edu.tau.eng.neuroscience.mri.machine;

import edu.tau.eng.neuroscience.mri.common.constants.MachineConstants;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * MachineServer run on each machine all the time, and listens to socket requests.
 * When a request arrive - the MachineServer create the required objects and call the MachineControllerImpl.
 */

public class MachineServer {
    private static Logger logger = LoggerManager.getLogger(MachineServer.class);

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(MachineConstants.MACHINE_SERVER_PORT);
        } catch (IOException e) {
            logger.error("Could not listen on port: " + MachineConstants.MACHINE_SERVER_PORT);
        }

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                RequestHandler rh = new RequestHandler(clientSocket);
                rh.start();
            }
        } catch (IOException|NullPointerException e) {
            logger.error("Machine server exception: " + e.getMessage());
        } finally {
            if (serverSocket != null)
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // do nothing
                }
        }

    }
}