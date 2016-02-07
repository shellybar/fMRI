package edu.tau.eng.neuroscience.mri.common.networkUtils;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;

public class SSHConnection {

    private static Logger logger = LoggerManager.getLogger(SSHConnection.class);

    public static Session establish(String sshHost, String user, String password) throws JSchException {
        int sshPort = 22;
        Session session;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, sshHost, sshPort);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("ConnectionAttempts", "3");
            logger.info("Establishing SSH Connection to host: " + sshHost + ":" + sshPort + "...");
            session.connect();
            logger.info("Connected to: " + sshHost + ":" + sshPort + " via SSH");
        }
        catch (JSchException e) {
            logger.error("SSH connection attempt to host: " + sshHost + ":" + sshPort + " failed");
            throw e;
        }
        return session;
    }

    public static Session establish() throws JSchException {
        // TODO read details from config file - eventually connection should use SSH-keys!
        String user = "razregev";
        String password = ""; // TODO Remove this before commit!!
        String sshHost = "nova.cs.tau.ac.il";
        return establish(sshHost, user, password);
    }

}
