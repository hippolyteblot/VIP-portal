package fr.insalyon.creatis.vip.datamanager.server.controller;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonView;

import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.server.inter.DataViews;
import fr.insalyon.creatis.vip.datamanager.models.Data;
import fr.insalyon.creatis.vip.datamanager.models.DataManagementError;
import fr.insalyon.creatis.vip.datamanager.models.PoolOperation;
import fr.insalyon.creatis.vip.datamanager.models.StorageDownloadRequest;
import fr.insalyon.creatis.vip.datamanager.server.business.StorageBusiness;

@RestController
@RequestMapping("/storage")
public class StorageController {

    private final StorageBusiness storageBusiness;
    @Autowired
    public StorageController(StorageBusiness storageBusiness) {
        this.storageBusiness = storageBusiness;
    }

    @GetMapping(value = {"/directories", "/directories/{*path}"})
    public List<Data> listDirectory(
            @PathVariable(name = "path", required = false) String path,
            @RequestParam(defaultValue = "false") boolean refresh) throws VipException {
        String resolvedPath = resolveStoragePath(path);
        return storageBusiness.listDir(resolvedPath);
    }

    private String resolveStoragePath(String path) throws VipException {
        if (path == null || path.isBlank()) {
            throw new VipException(DataManagementError.STORAGE_VALIDATION_ERROR, "Storage path is required");
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    // GET /storage/<path>
    @GetMapping(value = {"", "/", "/{*path}"})
    public Data getPathMetadata(@PathVariable(name = "path", required = false) String path) throws VipException {
        // TODO : Not implemented yet
        return new Data();
    }

    @JsonView(DataViews.User.class)
    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PoolOperation> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("destination") String destination) throws VipException, java.io.IOException {
        if (destination == null || destination.isBlank()) {
            throw new VipException(DataManagementError.STORAGE_VALIDATION_ERROR, "Upload destination is required");
        }
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new VipException(DataManagementError.STORAGE_VALIDATION_ERROR, "Upload file name is required");
        }
        
        String targetPath = buildUploadTargetPath(destination, file);
        String operationId = storageBusiness.submitUploadFromInputStream(
                targetPath,
                file.getInputStream(),
                file.getOriginalFilename());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
            .body(new PoolOperation(operationId, PoolOperation.Status.Queued));
    }

    @PostMapping(value = "/directories/{*path}")
    @ResponseStatus(HttpStatus.CREATED)
    public void createDirectory(@PathVariable String path) throws VipException {
        String fullPath = resolveStoragePath(path);
        storageBusiness.createDirectory(fullPath);
    }

    @JsonView(DataViews.User.class)
    @GetMapping(value = "/operations/{operationId}")
    public PoolOperation getOperationStatus(
            @PathVariable String operationId) throws VipException {
        PoolOperation.Status status = storageBusiness.getOperationStatus(operationId);
        return new PoolOperation(operationId, status);
    }

    @JsonView(DataViews.User.class)
    @PostMapping(value = "/downloads", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PoolOperation submitDownload(
            @RequestBody StorageDownloadRequest request) throws VipException {
        if (request == null || request.getPath() == null || request.getPath().isBlank()) {
            throw new VipException(DataManagementError.STORAGE_VALIDATION_ERROR, "Download path is required");
        }

        String operationId = storageBusiness.submitDownload(request.getPath());
        return new PoolOperation(operationId, PoolOperation.Status.Queued);
    }

    @GetMapping(value = "/downloads/{operationId}/content")
    public ResponseEntity<?> getDownloadContent(@PathVariable String operationId) throws VipException {
        File file = storageBusiness.getDownloadFileIfReady(operationId);
        if (file == null) {
            throw new VipException(DataManagementError.OPERATION_NOT_READY, "Download operation is not ready yet");
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.getName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(resource);
    }

    @DeleteMapping(value = "/{*path}")
    public ResponseEntity<Void> deleteFile(@PathVariable(name = "path") String path) throws VipException {
        String resolvedPath = resolveStoragePath(path);
        storageBusiness.deletePath(resolvedPath);
        return ResponseEntity.noContent().build();
    }

    private String buildUploadTargetPath(String destination, MultipartFile file) throws VipException {
        if (destination == null || destination.isBlank()) {
            throw new VipException(DataManagementError.STORAGE_VALIDATION_ERROR, "Upload destination is required");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new VipException(DataManagementError.STORAGE_VALIDATION_ERROR, "Upload file name is required");
        }

        String normalizedDestination = destination.startsWith("/") ? destination : "/" + destination;
        if (normalizedDestination.endsWith("/")) {
            return normalizedDestination + fileName;
        }
        return normalizedDestination + "/" + fileName;
    }
}
