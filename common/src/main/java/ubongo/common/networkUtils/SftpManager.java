package ubongo.common.networkUtils;

import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.exceptions.NetworkException;

import java.io.*;

import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.Selectors;

public class SftpManager {

    private static Logger logger = LogManager.getLogger(SftpManager.class);
    private String remoteDir;
    private String localDir;
    private String machine;
    private String user;
    private String password;
    private String sftpUri;
    private FileSystemManager fsManager = null;

    public SftpManager(SSHConnectionProperties sshProperties, String machine, String remoteDir, String localDir) throws NetworkException{
        this.machine = machine;
        this.remoteDir = remoteDir;
        this.localDir = localDir;
        //sshProperties.getUser(),
                //sshProperties.getPassword()
        this.user = sshProperties.getUser();
        this.password = sshProperties.getPassword();
        this.sftpUri = "sftp://" + user + ":" + password +  "@" + machine + remoteDir + "/";

        logger.info("SftpManager was initiated. Machine=" + this.machine+" remoteDir= "+this.remoteDir+ " destDir = " +this.localDir);
    }

    /**
     * Receives files using SFTP.
     * Used for receiving files from the main files server to the machines.
     */
    public void getFilesFromServer() throws NetworkException{
        try {
            FileSystemOptions opts = new FileSystemOptions();
            SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);
            fsManager = VFS.getManager();

            // List all the files in that directory.

            FileObject localFileObject=fsManager.resolveFile(sftpUri,opts);

            FileObject[] children = localFileObject.getChildren();
            for ( int i = 0; i < children.length; i++ ){
                String fileName = children[ i ].getName().getBaseName();
                System.out.println( fileName );
                String filepath = localDir + "/" + fileName;
                File file = new File(filepath);
                FileObject localFile = fsManager.resolveFile(file.getAbsolutePath(),opts);
                FileObject remoteFile = fsManager.resolveFile(sftpUri+ "/" + fileName, opts);
                localFile.copyFrom(remoteFile, Selectors.SELECT_SELF);
                logger.info("File downloaded successfully: " + fileName);
            }
        }
        catch (Exception ex) {
            throw new NetworkException(ex.getMessage());
        }
        return;
    }

    /**
     * Uploads files using SFTP.
     * Used for sending files from the machine to the main files server.
     */
    public void uploadFilesToServer() throws NetworkException{
        try {
            FileSystemOptions opts = new FileSystemOptions();
            SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);
            fsManager = VFS.getManager();

            File folder = new File(localDir);
            File[] listOfToUploadFiles = folder.listFiles();
            logger.debug("localDir = " + localDir);

            for (File fileToUpload : listOfToUploadFiles) {
                if (!fileToUpload.exists()) {
                    logger.error("Error. Local file not found : " + fileToUpload.getName());
                    throw new NetworkException("Error. Local file not found");
                }
                String fileName = fileToUpload.getName();
                logger.debug("uploading : " + fileName);
                FileObject localFile = fsManager.resolveFile(fileToUpload.getAbsolutePath(),opts);
                FileObject remoteFile = fsManager.resolveFile(sftpUri+ "/" + fileName, opts);
                logger.debug("local file object : " + localFile.getName());
                logger.debug("remote file object : " + remoteFile.getName());
                remoteFile.copyFrom(localFile, Selectors.SELECT_SELF);
                logger.info("File uploaded successfully: " + fileName);
            }
        }
        catch (Exception ex) {
            throw new NetworkException(ex.getMessage());
        }
        return;
    }

}
