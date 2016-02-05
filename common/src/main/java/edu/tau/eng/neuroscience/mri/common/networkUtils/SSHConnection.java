package edu.tau.eng.neuroscience.mri.common.networkUtils;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;

public class SSHConnection {

    private static Logger logger = LoggerManager.getLogger(SSHConnection.class);
    private static Session session;

    public static Session establish() throws JSchException {

        if (session != null && session.isConnected()) {
            return session;
        }

        // TODO read details from config file - eventually connection should use SSH-keys!
        String user = "";
        String password = "";
        String sshHost = "nova.cs.tau.ac.il";
        int sshPort = 22;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, sshHost, sshPort);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("ConnectionAttempts", "3");
            logger.info("Establishing SSH Connection to host: " + sshHost + ":" + sshPort + "...");
            session.connect();
        }
        catch (JSchException e) {
            logger.error("SSH connection attempt to host: " + sshHost + ":" + sshPort + " failed");
            throw e;
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (session != null && session.isConnected()) {
                    logger.info("Closing SSH connection");
                    session.disconnect();
                }
            }
        });
        return session;
    }

}
