package fr.insalyon.creatis.vip.api.business;

import static fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants.GROUP_APPEND;
import static fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants.ROOT;
import static fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants.TRASH_HOME;
import static fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants.USERS_HOME;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.insalyon.creatis.vip.api.exception.ApiError;
import fr.insalyon.creatis.vip.api.model.PathProperties;
import fr.insalyon.creatis.vip.api.model.UploadData;
import fr.insalyon.creatis.vip.api.model.UploadDataType;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.models.Group;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.Server;
import fr.insalyon.creatis.vip.datamanager.models.Data;
import fr.insalyon.creatis.vip.datamanager.server.business.LFCPermissionBusiness;
import fr.insalyon.creatis.vip.datamanager.server.business.LFCPermissionBusiness.LFCAccessType;
import fr.insalyon.creatis.vip.datamanager.server.business.StorageBusiness;

@Service
public class DataApiBusiness {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Server server;
    private final Supplier<User> currentUserProvider;

    private final LFCPermissionBusiness lfcPermissionBusiness;
    private final StorageBusiness storageBusiness;

    @Autowired
    public DataApiBusiness(
            Server server, Supplier<User> currentUserProvider,
            LFCPermissionBusiness lfcPermissionBusiness,
            StorageBusiness storageBusiness) {
        this.server = server;
        this.currentUserProvider = currentUserProvider;
        this.lfcPermissionBusiness = lfcPermissionBusiness;
        this.storageBusiness = storageBusiness;
    }

    public boolean doesFileExist(String path) throws VipException {
        checkReadPermission(path);
        return storageBusiness.doesFileExist(path);
    }

    public void deletePath(String path) throws VipException {
        checkPermission(path, LFCAccessType.DELETE);
        storageBusiness.deletePath(path);
    }

    public PathProperties getPathProperties(String path ) throws VipException {
        checkReadPermission(path);
        if (path.equals(ROOT)) {
            return getRootPathProperties();
        }
        PathProperties pathProperties = new PathProperties();
        pathProperties.setPath(path);
        Optional<Data.Type> type = storageBusiness.getPathInfo(path);
        if (type.isEmpty()) { // path doesn't exist
            pathProperties.setExists(false);
            return pathProperties;
        }
        pathProperties.setExists(true);
        List<Data> fileData = storageBusiness.getFileData(path);
        if (type.get().equals(Data.Type.file)) {
            // this is a file, not a directory
            Data fileInfo = fileData.get(0);
            pathProperties.setIsDirectory(false);
            pathProperties.setSize(fileInfo.getLength());
            pathProperties.setLastModificationDate(
                    getTimeStampFromGridaFormatDate(fileInfo.getModificationDate()));
            pathProperties.setMimeType(getMimeType(path));
        } else {
            // its a directory
            pathProperties.setIsDirectory(true);
            pathProperties.setSize((long) fileData.size());
            pathProperties.setLastModificationDate(
                storageBusiness.getModificationDate(path) / 1000);
            pathProperties.setMimeType(server.getCarminApiDirectoryMimeType());
        }
        return pathProperties;
    }

    public List<PathProperties> listDirectory(String path) throws VipException {
        checkReadPermission(path);
        if (path.equals(ROOT)) {
            return getRootSubDirectoriesPathProps();
        }
        Optional<Data.Type> type = storageBusiness.getPathInfo(path);
        if (!type.isPresent()) { // path doesn't exist
            logger.error("Trying to list a non-existing path ({})", path);
            throw new VipException("Error listing a directory");
        }
        if (!type.get().equals(Data.Type.folder)) {
            logger.error("Trying to list {} , but is a file :", path);
            throw new VipException("Error listing a directory");
        }
        List<Data> directoryData = storageBusiness.getFileData(path);
        List<PathProperties> res = new ArrayList<>();
        for (Data fileData : directoryData) {
            res.add(buildPathFromLfcData(path, fileData));
        }
        return res;
    }

    public File getFile(String path) throws VipException {
        checkDownloadPermission(path);
        return storageBusiness.getFile(path);
    }

    public void uploadRawFileFromInputStream(String lfcPath, InputStream is)
            throws VipException {
        // TODO : check upload size ?
        checkPermission(lfcPath, LFCAccessType.UPLOAD);
        java.nio.file.Path javaPath = Paths.get(lfcPath);
        String parentLfcPath = javaPath.getParent().toString();
        // check if parent dir exists
        if (!storageBusiness.doesFileExist(parentLfcPath)) {
            logger.error("parent directory of upload {} does not exist :", lfcPath);
            throw new VipException("Upload Directory doest not exist");
        }
        storageBusiness.uploadRawFileFromInputStream(lfcPath, is);
    }

    public void uploadCustomData(String lfcPath, UploadData uploadData)
            throws VipException {
        // TODO : check upload size ?
        // TODO : factorize with previous method
        checkPermission(lfcPath, LFCAccessType.UPLOAD);
        java.nio.file.Path javaPath = Paths.get(lfcPath);
        String parentLfcPath = javaPath.getParent().toString();
        // check if parent dir exists
        if (!storageBusiness.doesFileExist(parentLfcPath)) {
            logger.error("parent directory of {} does not exist :", lfcPath);
            throw new VipException("Upload Directory doest not exist");
        }
        if (uploadData.getType().equals(UploadDataType.ARCHIVE)) {
            logger.error("archive upload not supported yet for ({})", lfcPath);
            throw new VipException("archive upload not supported yet");
        }
        storageBusiness.uploadBase64File(lfcPath, uploadData.getBase64Content());
    }

    // #### PERMISSION STUFF

    private void checkReadPermission(String path) throws VipException {
        checkPermission(path, LFCAccessType.READ);
    }

    private void checkDownloadPermission(String path) throws VipException {
        checkReadPermission(path);
        if (path.equals(ROOT)) {
            logger.error("cannot download root ({})", path);
            throw new VipException("Illegal data API access");
        }
        Optional<Data.Type> type = storageBusiness.getPathInfo(path);
        if (!type.isPresent()) { // path doesn't exist
            logger.error("Trying to download a non-existing file ({})", path);
            throw new VipException("Illegal data API access");
        }
        if (!type.get().equals(Data.Type.file)) {
            // it works on a directory and return a zip, but we cant check the download size
            logger.error("Trying to download a directory ({})", path);
            throw new VipException("Illegal data API access");
        }
        // path exists and is a file: check its size
        List<Data> fileData = storageBusiness.getFileData(path);
        Long maxSize = server.getCarminApiDataTransfertMaxSize();
        if (fileData.get(0).getLength() > maxSize) {
            logger.error("Trying to download a file too big ({})", path);
            throw new VipException("Illegal data API access");
        }
    }

    private void checkPermission(String path, LFCAccessType accessType)
            throws VipException {
        if ( ! lfcPermissionBusiness.isLFCPathAllowed(
            currentUserProvider.get(), path, accessType, true)) {
            throw new VipException(ApiError.UNAUTHORIZED_DATA_ACCESS, path);
        }
    }

    // #### ROOT folder STUFF

    private PathProperties getRootPathProperties() {
        PathProperties rootPathProperties = new PathProperties();
        rootPathProperties.setExists(true);
        rootPathProperties.setMimeType(server.getCarminApiDirectoryMimeType());
        rootPathProperties.setIsDirectory(true);
        rootPathProperties.setSize((long) getRootDirectoriesName().size());
        rootPathProperties.setPath(ROOT);
        return rootPathProperties;
    }

    private List<PathProperties> getRootSubDirectoriesPathProps() {
        List<PathProperties> directories = new ArrayList<>();
        for (String dirName : getRootDirectoriesName()) {
            directories.add(getRootSubDirPathProperties(dirName));
        }
        return directories;
    }

    private List<String> getRootDirectoriesName() {
        // Home + Trash + users groups
        List<String> rootDir = new ArrayList<>();
        rootDir.add(USERS_HOME);
        rootDir.add(TRASH_HOME);
        for (Group group : currentUserProvider.get().getGroups()) {
            rootDir.add(group.getName() + GROUP_APPEND);
        }
        return rootDir;
    }

    private PathProperties getRootSubDirPathProperties(String name) {
        PathProperties rootPathProperties = new PathProperties();
        rootPathProperties.setExists(true);
        rootPathProperties.setMimeType(server.getCarminApiDirectoryMimeType());
        rootPathProperties.setIsDirectory(true);
        // TODO : size ?
        rootPathProperties.setPath(ROOT + "/" + name);
        return rootPathProperties;
    }

    // #### DATA UTILS

    private PathProperties buildPathFromLfcData(String path, Data lfcData) {
        PathProperties pathProperties = new PathProperties();
        pathProperties.setExists(true);
        pathProperties.setSize(lfcData.getLength());
        pathProperties.setLastModificationDate(
                getTimeStampFromGridaFormatDate(lfcData.getModificationDate()));
        boolean isDirectory = lfcData.getType().equals(Data.Type.folder)
                || lfcData.getType().equals(Data.Type.folderSync);
        pathProperties.setIsDirectory(isDirectory);
        if (isDirectory) {
            pathProperties.setMimeType(server.getCarminApiDirectoryMimeType());
        } else {
            pathProperties.setMimeType(getMimeType(lfcData.getName()));
        }
        pathProperties.setPath(path + "/" + lfcData.getName());
        return pathProperties;
    }

    /* returns timestamp in seconds from format "Jan 12 2016" */
    private Long getTimeStampFromGridaFormatDate(String gridaFormatDate) {
        if (gridaFormatDate == null || gridaFormatDate.isEmpty()) return null;
        DateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.US);
        try {
            return dateFormat.parse(gridaFormatDate).getTime() / 1000;
        } catch (ParseException e) {
            logger.warn("Error with grida date format : {}. Ignoring it", gridaFormatDate, e);
            return null;
        }
    }

    private String getMimeType(String path) {
        try {
            String contentType = Files.probeContentType(Paths.get(path));
            return contentType == null ? server.getCarminApiDefaultMimeType() : contentType;
        } catch (IOException e) {
            logger.warn("Cant detect mime type of {}. Ignoring and returning application/octet-stream",
                    path, e);
            return "application/octet-stream";
        }
    }

}
