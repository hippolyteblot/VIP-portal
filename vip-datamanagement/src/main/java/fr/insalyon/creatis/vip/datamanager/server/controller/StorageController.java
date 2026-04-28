package fr.insalyon.creatis.vip.datamanager.server.controller;

import java.io.File;
import java.net.URLDecoder;
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

import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants;
import fr.insalyon.creatis.vip.datamanager.models.Data;
import fr.insalyon.creatis.vip.datamanager.models.PoolOperation;
import fr.insalyon.creatis.vip.datamanager.models.StorageDownloadRequest;
import fr.insalyon.creatis.vip.datamanager.models.StorageOperationResponse;
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

    private String resolveStoragePath(String path) {
        if (path == null || path.isBlank()) {
            return DataManagerConstants.ROOT;
        }

        String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
        String normalized = decoded.replaceAll("/{2,}", "/");
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    // GET /storage/<path>
    @GetMapping(value = {"", "/", "/{*path}"})
    public Data getPathMetadata(@PathVariable(name = "path", required = false) String path) throws VipException {
        // TODO : Not implemented yet
        return new Data();
    }

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StorageOperationResponse> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("destination") String destination) throws VipException, java.io.IOException {
        String targetPath = buildUploadTargetPath(destination, file);
        String operationId = storageBusiness.submitUploadFromInputStream(
                targetPath,
                file.getInputStream(),
                file.getOriginalFilename());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new StorageOperationResponse(operationId, PoolOperation.Status.Queued.name()));
    }

    @PostMapping(value = "/directories/{*path}")
    @ResponseStatus(HttpStatus.CREATED)
    public void createDirectory(@PathVariable String path) throws VipException {
        String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
        String normalized = decoded.replaceAll("/{2,}", "/");
        String fullPath = normalized.startsWith("/") ? normalized : "/" + normalized;

        java.nio.file.Path javaPath = java.nio.file.Path.of(fullPath);
        String parentPath = javaPath.getParent() != null ? javaPath.getParent().toString() : "/";
        String name = javaPath.getFileName() != null ? javaPath.getFileName().toString() : "";

        storageBusiness.createDirectory(parentPath, name);
    }

    @GetMapping(value = "/operations/{operationId}")
    public StorageOperationResponse getOperationStatus(
            @PathVariable String operationId) throws VipException {
        PoolOperation.Status status = storageBusiness.getOperationStatus(operationId);
        return new StorageOperationResponse(operationId, status.name());
    }

    @PostMapping(value = "/downloads", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StorageOperationResponse> submitDownload(
            @RequestBody StorageDownloadRequest request) throws VipException {
        if (request == null || request.getPath() == null || request.getPath().isBlank()) {
            throw new VipException("Download path is required");
        }

        String operationId = storageBusiness.submitDownload(request.getPath());
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new StorageOperationResponse(operationId, PoolOperation.Status.Queued.name()));
    }

    @GetMapping(value = "/downloads/{operationId}/content")
    public ResponseEntity<?> getDownloadContent(@PathVariable String operationId) throws VipException {
        File file = storageBusiness.getDownloadFileIfReady(operationId);
        if (file == null) {
            PoolOperation.Status status = storageBusiness.getDownloadOperationStatus(operationId);
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(new StorageOperationResponse(operationId, status.name()));
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
            throw new VipException("Upload destination is required");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new VipException("Upload file name is required");
        }

        String normalizedDestination = destination.startsWith("/") ? destination : "/" + destination;
        if (normalizedDestination.endsWith("/")) {
            return normalizedDestination + fileName;
        }
        return normalizedDestination + "/" + fileName;
    }
}
