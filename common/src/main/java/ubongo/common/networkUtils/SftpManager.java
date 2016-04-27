package ubongo.common.networkUtils;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.exceptions.NetworkException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        this.user = sshProperties.getUser();
        this.password = sshProperties.getPassword();
        this.sftpUri = "sftp://" + user + ":" + password +  "@" + machine + remoteDir + File.separator;

        logger.info("SftpManager was initiated. Machine=" + this.machine+" remoteDir= "+this.remoteDir+ " localDir = " +this.localDir);
    }

    /**
     * Receives files using SFTP.
     * Used for receiving files from the main files server to the machines.
     */
    public void getFilesFromServer() throws NetworkException{
        Optional<String> fileRegex = getFileRegexFromInputDirRegex();
        List<String> localDirs = new ArrayList<>();
        try {
            String remoteDirPath = remoteDir;
            if (!remoteDir.endsWith(File.separator)){
                remoteDirPath = remoteDir.substring(0, remoteDir.lastIndexOf(File.separator));
            }
            logger.debug("getting directories for regex " + remoteDirPath);
            getDirectoriesFromRegex(localDirs, remoteDirPath);
        } catch (FileSystemException e) {
            String error = "Failed getting input files directories from regex";
            logger.error(error);
            throw new NetworkException(error);
        }
        for (String currLocalDir : localDirs) {
            logger.info("Downloading files from : " + currLocalDir);
            getFilesFromServerByDirectory(currLocalDir, fileRegex);
        }
    }

    private Optional<String> getFileRegexFromInputDirRegex() {
        if (remoteDir.endsWith(File.separator))
            return Optional.empty();
        String dirParts[] = remoteDir.split(File.separator);
        logger.info("Input files regex : " + dirParts[dirParts.length -1]);
        return Optional.of(dirParts[dirParts.length -1]);
    }

    private void getDirectoriesFromRegex(List<String> dirs, String currDir) throws FileSystemException {
        logger.debug("getDirectoriesFromRegex currDir: " + currDir);
        String dirParts[] = currDir.split(File.separator+"\\(.*?\\)"+File.separator);
        String prefixString = dirParts[0];
        if (dirParts.length == 1){
            dirs.add(prefixString);
            return;
        }
        String currRegex = currDir.substring(dirParts[0].length(), dirParts[1].length()-1);
        logger.debug("currRegex: " + currRegex);
        String suffixString = currDir.substring(currDir.indexOf(dirParts[1]));
        String currSftpUri = "sftp://" + user + ":" + password +  "@" + machine + prefixString + File.separator;
        fsManager = VFS.getManager();

        // List all the files in that directory.
        FileSystemOptions opts = new FileSystemOptions();
        FileObject localFileObject=fsManager.resolveFile(currSftpUri,opts);
        File file = new File(localFileObject.getName().getPath());
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        for ( String currSubDir : directories ){
            if (currSubDir.matches(currRegex)) {
                String currPath = prefixString + File.separator + currSubDir +File.separator + suffixString;
                String currDirSftpUri = "sftp://" + user + ":" + password + "@" + machine + currPath + File.separator;
                logger.debug("currDirSftpUri: " + currDirSftpUri);
                FileObject currDirObject = fsManager.resolveFile(currDirSftpUri, opts);
                logger.debug("currDirObject: " + currDirObject.getName());
                logger.debug("currDirObject: " + currDirObject.getURL());
                if (currDirObject.exists())
                    getDirectoriesFromRegex(dirs, currPath);
            } else {
                logger.debug("Directory " + currSubDir + " doesn't match pattern " + currRegex);
            }
        }
    }

    private void getFilesFromServerByDirectory(String currLocalDir, Optional<String> fileRegex) throws NetworkException{
        String currSftpUri = "sftp://" + user + ":" + password +  "@" + machine + currLocalDir + "/";
        try {
            FileSystemOptions opts = new FileSystemOptions();
            SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);
            fsManager = VFS.getManager();

            // List all the files in that directory.

            FileObject localFileObject=fsManager.resolveFile(currSftpUri,opts);

            FileObject[] children = localFileObject.getChildren();
            for ( int i = 0; i < children.length; i++ ){
                String fileName = children[ i ].getName().getBaseName();
                if ((fileRegex.isPresent()) && !(fileName.matches(fileRegex.get()))) {
                    logger.debug("File " + fileName + " doesn't match pattern " + fileRegex.get());
                    continue;
                }
                String filepath = currLocalDir + File.separator  + fileName;
                File file = new File(filepath);
                FileObject localFile = fsManager.resolveFile(file.getAbsolutePath(),opts);
                FileObject remoteFile = fsManager.resolveFile(currSftpUri+ File.separator + fileName, opts);
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
                FileObject localFile = fsManager.resolveFile(fileToUpload.getAbsolutePath(),opts);
                FileObject remoteFile = fsManager.resolveFile(sftpUri+ File.separator + fileName, opts);
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
