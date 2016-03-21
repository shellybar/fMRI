package ubongo.common.networkUtils;


import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import ubongo.common.exceptions.NetworkException;
import ubongo.common.log.Logger;
import ubongo.common.log.LoggerManager;

import java.io.*;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.Selectors;

public class FilesClient {

    private static Logger logger = LoggerManager.getLogger(FilesClient.class);
    private String sourceDir;
    private String destDir;
    private String sourceMachine;
    private String user;
    private String password;
    private String sftpUri;
    private FileSystemManager fsManager = null;

    public FilesClient(String sourceMachine, String sourceDir, String destDir) throws NetworkException{
        this.sourceMachine = sourceMachine;
        this.sourceDir = sourceDir;
        this.destDir = destDir;
        this.user = "shellybar"; // TODO get from conf - create a new user;
        this.password = "xxx"; // TODO get from conf - create a new user;
        this.sftpUri = "sftp://" + user + ":" + password +  "@" + sourceMachine + sourceDir + "/";

        logger.info("FilesClient was initiated. sourceMachine=" + sourceMachine+" sourceDir= "+sourceDir+ " destDir = " +destDir);
    }


    /**
     * Receives files using SFTP.
     * Used for receiving files from the main files server to the machines, or from a machine to the files server.
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
                String filepath = destDir + "/" + fileName;
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


}
