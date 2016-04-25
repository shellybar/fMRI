package ubongo.common.networkUtils;

import org.apache.commons.vfs2.impl.StandardFileSystemManager;
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

    public SftpManager(String machine, String remoteDir, String localDir) throws NetworkException{
        this.machine = machine;
        this.remoteDir = remoteDir;
        this.localDir = localDir;
        //sshProperties.getUser(),
                //sshProperties.getPassword()
        this.user = "razregev"; // TODO get from conf - create a new user;
        this.password = ""; // TODO get from conf - create a new user;
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

            // List all the files in that directory.Try to give the directory path

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
                logger.info("File download successful: " + fileName);
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
            StandardFileSystemManager manager = new StandardFileSystemManager();
            //Initializes the file manager
            manager.init();
            File folder = new File(localDir);
            File[] listOfToUploadFiles = folder.listFiles();

            for (File fileToUpload : listOfToUploadFiles){
                if (!fileToUpload.exists())
                    throw new NetworkException("Error. Local file not found");
                String fileName = fileToUpload.getName();

                //Setup our SFTP configuration
                FileSystemOptions opts = new FileSystemOptions();
                SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(
                        opts, "no");
                SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, true);
                SftpFileSystemConfigBuilder.getInstance().setTimeout(opts, 10000);

                String sftpUri = this.sftpUri + fileToUpload;
                // Create local file object
                FileObject localFile = manager.resolveFile(fileToUpload.getAbsolutePath());

                // Create remote file object
                FileObject remoteFile = manager.resolveFile(sftpUri, opts);

                // Copy local file to sftp server
                remoteFile.copyFrom(localFile, Selectors.SELECT_SELF);
                System.out.println("File upload successful " + fileName);
            }
        }
        catch (Exception ex) {
            throw new NetworkException(ex.getMessage());
        }
        return;
    }


}
