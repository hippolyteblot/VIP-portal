package fr.insalyon.creatis.vip.datamanager.server.business;

import static fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants.GROUP_APPEND;
import static fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants.ROOT;
import static fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants.TRASH_HOME;
import static fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants.USERS_HOME;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import fr.insalyon.creatis.vip.datamanager.models.VipStoragePath;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.server.dao.GroupDAO;
import fr.insalyon.creatis.vip.core.models.Group;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.Server;
import fr.insalyon.creatis.vip.datamanager.models.Data;
import fr.insalyon.creatis.vip.datamanager.models.PoolOperation;
import fr.insalyon.creatis.vip.datamanager.server.DataManagerUtil;
import fr.insalyon.creatis.vip.datamanager.server.business.LFCPermissionBusiness.LFCAccessType;

@Service
@Transactional
public class StorageBusiness {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Server server;
    private final LFCBusiness lfcBusiness;
    private final LFCPermissionBusiness lfcPermissionBusiness;
    private final Supplier<User> currentUserProvider;
    private final GroupDAO groupDAO;
    private final TransferPoolBusiness transferPoolBusiness;
    private final DataManagerBusiness dataManagerBusiness;
    private final ScheduledExecutorService scheduler;
    private final VipStoragePathFactory vipStoragePathFactory;

    @Autowired
    public StorageBusiness(
            Server server,
            LFCBusiness lfcBusiness,
            LFCPermissionBusiness lfcPermissionBusiness,
            Supplier<User> currentUserProvider,
            GroupDAO groupDAO,
            ScheduledExecutorService scheduler,
            TransferPoolBusiness transferPoolBusiness,
            DataManagerBusiness dataManagerBusiness,
            VipStoragePathFactory vipStoragePathFactory) {
        this.server = server;
        this.lfcBusiness = lfcBusiness;
        this.lfcPermissionBusiness = lfcPermissionBusiness;
        this.currentUserProvider = currentUserProvider;
        this.groupDAO = groupDAO;
        this.scheduler = scheduler;
        this.transferPoolBusiness = transferPoolBusiness;
        this.dataManagerBusiness = dataManagerBusiness;
        this.vipStoragePathFactory = vipStoragePathFactory;
    }

    public boolean doesFileExist(String path) throws VipException {
        path = normalizeVipPath(path);
        checkReadPermission(path);
        return path.equals(ROOT) || lfcBusiness.exists(currentUserProvider.get(), path);
    }

    public List<Data> listDir(String path) throws VipException {
        path = normalizeVipPath(path);

        checkReadPermission(path);
        if (path.equals(ROOT)) {
            return getRootDirectoriesName().stream()
                    .map(name -> new Data(name, Data.Type.folder, null))
                    .toList();
        }
        Optional<Data.Type> type = lfcBusiness.getPathInfo(currentUserProvider.get(), path);
        if (type.isEmpty()) {
            if (isValidGroupPath(path)) {
                lfcBusiness.ensureDirectoryExists(currentUserProvider.get(), path);
                type = lfcBusiness.getPathInfo(currentUserProvider.get(), path);
            }
        }
        if (type.isEmpty()) {
            logger.error("Trying to list a non-existing path ({})", path);
            throw new VipException("Error listing a directory");
        }
        if (!type.get().equals(Data.Type.folder)) {
            logger.error("Trying to list {} , but is a file :", path);
            throw new VipException("Error listing a directory");
        }
        return lfcBusiness.listDir(currentUserProvider.get(), path, true);
    }

    public Optional<Data.Type> getPathInfo(String path) throws VipException {
        path = normalizeVipPath(path);
        checkReadPermission(path);
        if (path.equals(ROOT)) {
            return Optional.of(Data.Type.folder);
        }
        return lfcBusiness.getPathInfo(currentUserProvider.get(), path);
    }

    public List<Data> getFileData(String path) throws VipException {
        path = normalizeVipPath(path);
        checkReadPermission(path);
        if (path.equals(ROOT)) {
            return listDir(path);
        }
        return lfcBusiness.listDir(currentUserProvider.get(), path, true);
    }

    public Long getModificationDate(String path) throws VipException {
        path = normalizeVipPath(path);
        checkReadPermission(path);
        if (path.equals(ROOT)) {
            return null;
        }
        return lfcBusiness.getModificationDate(currentUserProvider.get(), path);
    }

    public void deletePath(String path) throws VipException {
        path = normalizeVipPath(path);
        checkPermission(path, LFCAccessType.DELETE);
        if (!lfcBusiness.exists(currentUserProvider.get(), path)) {
            logger.error("trying to delete a non-existing file : {}", path);
            throw new VipException("trying to delete a non-existing file");
        }
        transferPoolBusiness.delete(currentUserProvider.get(), path);
    }

    public void createDirectory(String parentPath, String name) throws VipException {
        String normalizedParent = normalizeVipPath(parentPath);
        String normalizedName = name == null ? "" : name.trim();

        if (normalizedName.isEmpty() || normalizedName.contains("/")) {
            throw new VipException("Directory name is invalid");
        }

        String targetPath = normalizedParent.endsWith("/")
                ? normalizedParent + normalizedName
                : normalizedParent + "/" + normalizedName;

        checkPermission(targetPath, LFCAccessType.UPLOAD);
        lfcBusiness.createDir(currentUserProvider.get(), normalizedParent, normalizedName);
    }

    public File getFile(String path) throws VipException {
        path = normalizeVipPath(path);
        checkDownloadPermission(path);
        String downloadOperationId = downloadFileToLocalStorage(path);
        return getDownloadFile(downloadOperationId);
    }

    public String submitUploadFromInputStream(
            String lfcPath,
            InputStream inputStream,
            String originalFileName) throws VipException {
        checkPermission(lfcPath, LFCAccessType.UPLOAD);

        java.nio.file.Path javaPath = Paths.get(lfcPath);
        String parentLfcPath = javaPath.getParent().toString();
        ensureUploadParentExists(parentLfcPath, lfcPath);

        String uploadDirectory = dataManagerBusiness.getUploadRootDirectory(false);
        String sourceFileName = originalFileName != null && !originalFileName.isBlank()
                ? originalFileName
                : javaPath.getFileName().toString();
        String cleanFileName = DataManagerUtil.getCleanFilename(sourceFileName);
        String localPath = buildUniqueLocalUploadPath(uploadDirectory, cleanFileName);

        saveInputStreamToFile(inputStream, localPath);
        return transferPoolBusiness.uploadFile(currentUserProvider.get(), localPath, parentLfcPath);
    }

    public String submitDownload(String path) throws VipException {
        path = normalizeVipPath(path);
        checkDownloadPermission(path);
        return transferPoolBusiness.downloadFile(currentUserProvider.get(), path);
    }

    public PoolOperation.Status getOperationStatus(String operationId) throws VipException {
        PoolOperation operation = transferPoolBusiness.getOperationById(
                operationId,
                currentUserProvider.get().getFolder());
        ensureOperationOwnership(operation, operationId);
        return operation.getStatus();
    }

    public File getDownloadFileByOperationId(String operationId) throws VipException {
        PoolOperation operation = transferPoolBusiness.getOperationById(
                operationId,
                currentUserProvider.get().getFolder());
        ensureOperationOwnership(operation, operationId);

        if (!PoolOperation.Type.Download.equals(operation.getType())) {
            throw new VipException("Operation is not a download");
        }

        PoolOperation downloadOperation = transferPoolBusiness.getDownloadPoolOperation(operationId);
        if (!PoolOperation.Status.Done.equals(downloadOperation.getStatus())) {
            throw new VipException("Download operation not completed");
        }

        return getDownloadFile(operationId);
    }

    public void uploadRawFileFromInputStream(String lfcPath, InputStream is)
            throws VipException {
        checkPermission(lfcPath, LFCAccessType.UPLOAD);

        java.nio.file.Path javaPath = Paths.get(lfcPath);
        String parentLfcPath = javaPath.getParent().toString();

        ensureUploadParentExists(parentLfcPath, lfcPath);

        String uploadDirectory = dataManagerBusiness.getUploadRootDirectory(false);
        String fileName = DataManagerUtil.getCleanFilename(javaPath.getFileName().toString());
        // TODO : handle potential file name conflict in upload directory
        String localPath = uploadDirectory + fileName;

        logger.debug("storing upload file in : {}", localPath);
        boolean isFileEmpty = saveInputStreamToFile(is, localPath);

        if (isFileEmpty) {
            logger.info("no content in upload, creating dir : {}/{}", parentLfcPath, fileName);
            lfcBusiness.createDir(currentUserProvider.get(), parentLfcPath, fileName);
        } else {
            String opId = transferPoolBusiness.uploadFile(currentUserProvider.get(), localPath, parentLfcPath);
            waitForOperationOrTimeout(opId);
        }
    }

    public void uploadBase64File(String lfcPath, String base64Content) throws VipException {
        checkPermission(lfcPath, LFCAccessType.UPLOAD);

        java.nio.file.Path javaPath = Paths.get(lfcPath);
        String parentLfcPath = javaPath.getParent().toString();

        ensureUploadParentExists(parentLfcPath, lfcPath);

        String uploadDirectory = dataManagerBusiness.getUploadRootDirectory(false);
        String fileName = DataManagerUtil.getCleanFilename(javaPath.getFileName().toString());
        String localPath = uploadDirectory + fileName;

        logger.debug("storing upload file in : {}", localPath);
        writeFileFromBase64(base64Content, localPath);

        String opId = transferPoolBusiness.uploadFile(currentUserProvider.get(), localPath, parentLfcPath);
        waitForOperationOrTimeout(opId);
    }

    public List<String> getRootDirectoriesName() throws VipException {
        List<String> rootDir = new ArrayList<>();
        rootDir.add(USERS_HOME);
        rootDir.add(TRASH_HOME);

        List<Group> groups;
        if (currentUserProvider.get().isGroupAdmin()) {
            groups = groupDAO.get();
        } else {
            groups = new ArrayList<>(currentUserProvider.get().getGroups());
        }

        for (Group group : groups) {
            logger.info("Adding group {} to root dir", group.getName());
            rootDir.add(group.getName() + GROUP_APPEND);
        }
        return rootDir;
    }

    private void checkReadPermission(String path) throws VipException {
        checkPermission(path, LFCAccessType.READ);
    }

    private String normalizeVipPath(String path) {
        VipStoragePath vipPath = vipStoragePathFactory.create(currentUserProvider.get(), path);
        return vipPath.getVipPath();
    }

    private boolean isValidGroupPath(String path) throws VipException {
        if (path == null || !path.startsWith(ROOT + "/")) {
            return false;
        }

        String remaining = path.substring((ROOT + "/").length());
        int slashIndex = remaining.indexOf('/');
        String firstDir = slashIndex >= 0 ? remaining.substring(0, slashIndex) : remaining;

        if (!firstDir.endsWith(GROUP_APPEND)) {
            return false;
        }

        String groupName = firstDir.substring(0, firstDir.length() - GROUP_APPEND.length());
        List<Group> groups = currentUserProvider.get().isGroupAdmin()
                ? groupDAO.get()
                : new ArrayList<>(currentUserProvider.get().getGroups());

        return groups.stream().anyMatch(group -> groupName.equals(group.getName()));
    }

    private void checkDownloadPermission(String path) throws VipException {
        checkReadPermission(path);
        if (path.equals(ROOT)) {
            logger.error("cannot download root ({})", path);
            throw new VipException("Illegal data API access");
        }
        Optional<Data.Type> type = lfcBusiness.getPathInfo(currentUserProvider.get(), path);
        if (type.isEmpty()) {
            logger.error("Trying to download a non-existing file ({})", path);
            throw new VipException("Illegal data API access");
        }
        if (!type.get().equals(Data.Type.file)) {
            logger.error("Trying to download a directory ({})", path);
            throw new VipException("Illegal data API access");
        }

        Long maxSize = server.getCarminApiDataTransfertMaxSize();
        if (maxSize != null && maxSize > 0) {
            List<Data> fileData = lfcBusiness.listDir(currentUserProvider.get(), path, true);
            if (!fileData.isEmpty() && fileData.get(0).getLength() > maxSize) {
                logger.error("Trying to download a file too big ({})", path);
                throw new VipException("Illegal data API access");
            }
        }
    }

    private void checkPermission(String path, LFCAccessType accessType)
            throws VipException {
        if (!lfcPermissionBusiness.isLFCPathAllowed(
                currentUserProvider.get(), path, accessType, true)) {
            throw new VipException("Permission denied for path " + path);
        }
    }

    private String downloadFileToLocalStorage(String path) throws VipException {
        String downloadOperationId = transferPoolBusiness.downloadFile(currentUserProvider.get(), path);
        waitForOperationOrTimeout(downloadOperationId);
        return downloadOperationId;
    }

    private File getDownloadFile(String operationId) throws VipException {
        PoolOperation operation = transferPoolBusiness.getDownloadPoolOperation(operationId);
        File file = new File(operation.getDest());
        if (file.isDirectory()) {
            file = new File(operation.getDest() + "/"
                    + FilenameUtils.getName(operation.getSource()));
        }
        return file;
    }

    private void ensureUploadParentExists(String parentLfcPath, String lfcPath) throws VipException {
        if (!lfcBusiness.exists(currentUserProvider.get(), parentLfcPath)) {
            if (isValidGroupPath(parentLfcPath)) {
                lfcBusiness.ensureDirectoryExists(currentUserProvider.get(), parentLfcPath);
            } else {
                logger.error("parent directory of upload {} does not exist :", lfcPath);
                throw new VipException("Upload directory does not exist");
            }
        }
    }

    private String buildUniqueLocalUploadPath(String uploadDirectory, String cleanFileName) {
        return uploadDirectory + System.currentTimeMillis() + "_" + cleanFileName;
    }

    private void ensureOperationOwnership(PoolOperation operation, String operationId) throws VipException {
        String currentUserEmail = currentUserProvider.get().getEmail();
        if (operation.getUser() == null || !operation.getUser().equals(currentUserEmail)) {
            logger.error("Operation {} does not belong to current user {}", operationId, currentUserEmail);
            throw new VipException("Operation not accessible");
        }
    }

    private void waitForOperationOrTimeout(String operationId)
            throws VipException {
        User user = currentUserProvider.get();
        Callable<Boolean> isDownloadOverCall =
                () -> isOperationOver(operationId, user);

        Callable<Boolean> waitForDownloadCall = () -> {
            while (true) {
                Future<Boolean> isDownloadOverFuture =
                        scheduler.schedule(isDownloadOverCall, getRetryDelay(), TimeUnit.SECONDS);
                if (isDownloadOverFuture.get()) {
                    return true;
                }
            }
        };

        Future<Boolean> completionFuture = scheduler.submit(waitForDownloadCall);
        timeoutOperationCompletionFuture(operationId, completionFuture, getTimeout());
    }

    private void timeoutOperationCompletionFuture(
            String operationId,
            Future<Boolean> completionFuture, int timeoutInSeconds) throws VipException {
        try {
            completionFuture.get(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Waiting for operation completion interrupted : {}", operationId, e);
            throw new VipException("Waiting for operation completion interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Error waiting for operation completion : {}", operationId, e);
            throw new VipException("Error waiting for operation completion", e);
        } catch (TimeoutException e) {
            completionFuture.cancel(true);
            logger.error("Timeout operation completion : {}", operationId, e);
            throw new VipException("Aborting operation : too long", e);
        }
    }

    private Integer getRetryDelay() {
        return server.getCarminApiDownloadRetryInSeconds();
    }

    private Integer getTimeout() {
        return server.getCarminApiDownloadTimeoutInSeconds();
    }

    private boolean isOperationOver(String operationId, User user)
            throws VipException {
        PoolOperation operation = transferPoolBusiness.getOperationById(operationId, user.getFolder());

        switch (operation.getStatus()) {
            case Queued:
            case Running:
                logger.debug("status of operation {} : {}", operationId, operation.getStatus());
                return false;
            case Done:
                return true;
            case Failed:
            case Rescheduled:
            default:
                logger.error("IO LFC Operation failed : {} : {}", operationId, operation.getStatus());
                throw new VipException("IO LFC Operation operation failed");
        }
    }

    private void writeFileFromBase64(String base64Content, String localFilePath) throws VipException {
        Base64.Decoder decoder = Base64.getDecoder();
        try (
                InputStream inputStream = ReaderInputStream.builder()
                        .setReader(new StringReader(base64Content))
                        .setCharset(StandardCharsets.UTF_8)
                        .get();
                InputStream base64InputStream = decoder.wrap(inputStream)) {
            saveInputStreamToFile(base64InputStream, localFilePath);
        } catch (IOException e) {
            logger.error("Error writing base64 file in {}", localFilePath, e);
            throw new VipException("Error writing base64 file", e);
        }
    }

    private boolean saveInputStreamToFile(InputStream is, String path) throws VipException {
        Long maxSize = server.getCarminApiDataTransfertMaxSize();
        long totalBytesRead = 0;

        try (OutputStream fos = Files.newOutputStream(Paths.get(path))) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            boolean isFileEmpty = true;
            while ((bytesRead = is.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                if (maxSize != null && maxSize > 0 && totalBytesRead > maxSize) {
                    logger.error("Trying to upload a file too big ({} bytes, max {} bytes)",
                            totalBytesRead, maxSize);
                    throw new VipException(DefaultError.FILE_TOO_LARGE, maxSize);
                }
                isFileEmpty = false;
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush();
            return isFileEmpty;
        } catch (VipException e) {
            cleanupLocalUploadFile(path);
            throw e;
        } catch (IOException e) {
            logger.error("IO Error storing file {}", path, e);
            throw new VipException("Upload error", e);
        }
    }

    private void cleanupLocalUploadFile(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException cleanupException) {
            logger.warn("Could not cleanup temporary upload file {}", path, cleanupException);
        }
    }
}