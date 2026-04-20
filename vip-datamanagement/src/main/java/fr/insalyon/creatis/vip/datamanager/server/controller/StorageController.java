package fr.insalyon.creatis.vip.datamanager.server.controller;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants;
import fr.insalyon.creatis.vip.datamanager.models.Data;
import fr.insalyon.creatis.vip.datamanager.models.PoolOperation;
import fr.insalyon.creatis.vip.datamanager.server.controller.dto.StorageCreateDirectoryRequest;
import fr.insalyon.creatis.vip.datamanager.server.controller.dto.StorageDownloadRequest;
import fr.insalyon.creatis.vip.datamanager.server.controller.dto.StorageOperationResponse;
import fr.insalyon.creatis.vip.datamanager.server.business.StorageBusiness;

@RestController
@RequestMapping("/storage")
public class StorageController {

    private final StorageBusiness storageBusiness;
    @Autowired
    public StorageController(StorageBusiness storageBusiness) {
        this.storageBusiness = storageBusiness;
    }

    // GET /storage/directories/<path>?refresh=<bool>
    @GetMapping(value = "/directories/**")
    public List<Data> listStoragePath(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") boolean refresh) throws VipException {
        String resolvedPath = resolvePath(request);
        List<Data> children = storageBusiness.listDir(resolvedPath);
        return children;
    }

    @PostMapping(value = "/directories", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createDirectory(@RequestBody StorageCreateDirectoryRequest request) throws VipException {
        if (request == null || request.getPath() == null || request.getPath().isBlank()) {
            throw new VipException("Directory path is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new VipException("Directory name is required");
        }

        storageBusiness.createDirectory(request.getPath(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private String resolvePath(HttpServletRequest request) {
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String pathWithinHandlerMapping = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        if (bestMatchPattern != null && pathWithinHandlerMapping != null) {
            String extracted = new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, pathWithinHandlerMapping);
            if (extracted != null && !extracted.isBlank()) {
                String decoded = URLDecoder.decode(extracted, StandardCharsets.UTF_8);
                String normalized = decoded.replaceAll("/{2,}", "/");
                return normalized.startsWith("/") ? normalized : "/" + normalized;
            }
        }

        return DataManagerConstants.ROOT;
    }

    @GetMapping(value = "/**", params = "download")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) throws VipException {
        String path = resolvePath(request);
        File file = storageBusiness.getFile(path);

        Resource resource = new FileSystemResource(file);
        String fileName = file.getName();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(resource);
    }

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StorageOperationResponse> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "path", required = false) String path) throws VipException, java.io.IOException {
        String targetPath = resolveUploadTargetPath(path, file);
        String operationId = storageBusiness.submitUploadFromInputStream(
                targetPath,
                file.getInputStream(),
            file.getOriginalFilename());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new StorageOperationResponse(operationId, PoolOperation.Status.Queued.name()));
    }

    @GetMapping(value = "/operations/{operationId}")
    public ResponseEntity<StorageOperationResponse> getOperationStatus(
            @PathVariable String operationId) throws VipException {
        PoolOperation.Status status = storageBusiness.getOperationStatus(operationId);
        return ResponseEntity.ok(new StorageOperationResponse(operationId, status.name()));
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
        PoolOperation.Status status = storageBusiness.getOperationStatus(operationId);
        if (!PoolOperation.Status.Done.equals(status)) {
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(new StorageOperationResponse(operationId, status.name()));
        }

        File file = storageBusiness.getDownloadFileByOperationId(operationId);
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

    @DeleteMapping(value = "/**")
    public ResponseEntity<Void> deleteFile(HttpServletRequest request) throws VipException {
        String path = resolvePath(request);
        storageBusiness.deletePath(path);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/directories/**")
    public ResponseEntity<Void> deleteDirectory(HttpServletRequest request) throws VipException {
        String path = resolvePath(request);
        storageBusiness.deletePath(path);
        return ResponseEntity.noContent().build();
    }

    private String resolveUploadTargetPath(
            String requestParamPath,
            MultipartFile file) throws VipException {
        String rawPath = requestParamPath;

        if (rawPath == null || rawPath.isBlank()) {
            throw new VipException("Upload path is required");
        }

        String normalized = rawPath.startsWith("/") ? rawPath : "/" + rawPath;
        if (normalized.endsWith("/")) {
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                throw new VipException("Upload file name is required");
            }
            return normalized + fileName;
        }

        return normalized;
    }
}