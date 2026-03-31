package fr.insalyon.creatis.vip.datamanager.server.controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.datamanager.models.Data;
import fr.insalyon.creatis.vip.datamanager.server.business.DataManagerBusiness;
import fr.insalyon.creatis.vip.datamanager.server.business.LFCBusiness;
import fr.insalyon.creatis.vip.datamanager.server.business.TransferPoolBusiness;
import jakarta.validation.Valid;

@RestController
@RequestMapping
public class NodeController {

    private final LFCBusiness lfcBusiness;
    private final TransferPoolBusiness transferPoolBusiness;
    private final DataManagerBusiness dataManagerBusiness;
    private final Supplier<User> userProvider;

    @Autowired
    public NodeController(LFCBusiness lfcBusiness, TransferPoolBusiness transferPoolBusiness,
            DataManagerBusiness dataManagerBusiness, Supplier<User> userProvider) {
        this.lfcBusiness = lfcBusiness;
        this.transferPoolBusiness = transferPoolBusiness;
        this.dataManagerBusiness = dataManagerBusiness;
        this.userProvider = userProvider;
    }

    @GetMapping("/entries/{id}")
    public EntryResponse getEntry(@PathVariable String id) throws VipException {
        String path = normalizePath(id);
        return buildEntry(path);
    }

    @GetMapping("/entries/{id}/children")
    public List<EntryResponse> listChildren(@PathVariable String id,
            @RequestParam(defaultValue = "false") boolean refresh) throws VipException {
        String basePath = normalizePath(id);

        Optional<Data.Type> type = lfcBusiness.getPathInfo(currentUser(), basePath);
        if (type.isEmpty()) {
            throw new VipException(DefaultError.NOT_FOUND, basePath);
        }
        if (type.get() != Data.Type.folder && type.get() != Data.Type.folderSync) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, basePath, "Entry is not a folder");
        }

        return lfcBusiness.listDir(currentUser(), basePath, refresh).stream()
                .map(child -> toEntryResponse(basePath, child))
                .toList();
    }

    @PostMapping("/entries")
    public EntryResponse createEntry(@RequestBody @Valid CreateEntryRequest request) throws VipException {
        String parentPath = normalizePath(request.parentId());
        String entryName = request.name();
        if (entryName == null || entryName.isBlank()) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "name", "Entry name is required");
        }

        String targetPath = join(parentPath, entryName);
        String type = request.type() == null ? "folder" : request.type().trim().toLowerCase();

        if ("folder".equals(type)) {
            lfcBusiness.createDir(currentUser(), parentPath, entryName);
        } else if ("file".equals(type)) {
            writeFileContent(targetPath, request.content() == null ? "" : request.content());
        } else {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "type", "Supported values are 'file' and 'folder'");
        }

        return buildEntry(targetPath);
    }

    @PatchMapping("/entries/{id}")
    public EntryResponse patchEntry(@PathVariable String id, @RequestBody @Valid UpdateEntryRequest request)
            throws VipException {
        String sourcePath = normalizePath(id);

        String targetPath;
        if (request.newPath() != null && !request.newPath().isBlank()) {
            targetPath = normalizePath(request.newPath());
        } else if (request.newName() != null && !request.newName().isBlank()) {
            targetPath = join(parentPath(sourcePath), request.newName());
        } else {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "newPath/newName",
                    "Either newPath or newName must be provided");
        }

        lfcBusiness.rename(currentUser(), sourcePath, targetPath, Boolean.TRUE.equals(request.extendPath()));
        return buildEntry(targetPath);
    }

    @DeleteMapping("/entries/{id}")
    public void deleteEntry(@PathVariable String id) throws VipException {
        String path = normalizePath(id);
        transferPoolBusiness.delete(currentUser(), path);
    }

    @GetMapping("/files/{id}/content")
    public ResponseEntity<byte[]> getFileContent(@PathVariable String id) throws VipException {
        String path = normalizePath(id);
        String localPath = dataManagerBusiness.getRemoteFile(currentUser(), path);
        File localFile = new File(localPath);

        try {
            byte[] content = Files.readAllBytes(localFile.toPath());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + localFile.getName() + "\"")
                    .body(content);
        } catch (IOException e) {
            throw new VipException("Cannot read downloaded file content", e);
        } finally {
            dataManagerBusiness.deleteLocalFile(localPath);
        }
    }

    @PutMapping("/files/{id}/content")
    public EntryResponse putFileContent(@PathVariable String id, @RequestBody byte[] content)
            throws VipException {
        String path = normalizePath(id);
        writeFileBytes(path, content == null ? new byte[0] : content);
        return buildEntry(path);
    }

    private User currentUser() {
        return userProvider.get();
    }

    private EntryResponse buildEntry(String path) throws VipException {
        Optional<Data.Type> type = lfcBusiness.getPathInfo(currentUser(), path);
        if (type.isEmpty()) {
            throw new VipException(DefaultError.NOT_FOUND, path);
        }

        String parentPath = parentPath(path);
        String name = entryName(path);

        List<Data> siblings = lfcBusiness.listDir(currentUser(), parentPath, false);
        for (Data sibling : siblings) {
            if (name.equals(sibling.getName())) {
                return toEntryResponse(parentPath, sibling);
            }
        }

        boolean directory = type.get() == Data.Type.folder || type.get() == Data.Type.folderSync;
        return new EntryResponse(path, name, directory, 0L, "", "", List.of());
    }

    private EntryResponse toEntryResponse(String parentPath, Data data) {
        boolean directory = data.getType() == Data.Type.folder || data.getType() == Data.Type.folderSync;
        return new EntryResponse(
                join(parentPath, data.getName()),
                data.getName(),
                directory,
                data.getLength(),
                data.getModificationDate(),
                data.getPermissions(),
                data.getReplicas() == null ? List.of() : data.getReplicas());
    }

    private void writeFileContent(String targetPath, String content) throws VipException {
        writeFileBytes(targetPath, content.getBytes(StandardCharsets.UTF_8));
    }

    private void writeFileBytes(String targetPath, byte[] content) throws VipException {
        java.nio.file.Path tempFile = null;
        try {
            tempFile = Files.createTempFile("vip-node-", ".tmp");
            Files.write(tempFile, content);
            transferPoolBusiness.uploadFile(currentUser(), tempFile.toString(), targetPath);
        } catch (IOException e) {
            throw new VipException("Cannot write temporary file for upload", e);
        } finally {
            if (tempFile != null) {
                dataManagerBusiness.deleteLocalFile(tempFile.toString());
            }
        }
    }

    private String normalizePath(String id) {
        if (id == null || id.isBlank()) {
            return "/vip";
        }
        String normalized = id.trim().replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (!normalized.startsWith("/vip")) {
            normalized = "/vip" + normalized;
        }
        return normalized;
    }

    private String parentPath(String path) {
        int idx = path.lastIndexOf('/');
        if (idx <= 0) {
            return "/vip";
        }
        String parent = path.substring(0, idx);
        return parent.isBlank() ? "/vip" : parent;
    }

    private String entryName(String path) {
        int idx = path.lastIndexOf('/');
        if (idx < 0 || idx == path.length() - 1) {
            return path;
        }
        return path.substring(idx + 1);
    }

    private String join(String basePath, String name) {
        if (basePath.endsWith("/")) {
            return (basePath + name).replaceAll("/{2,}", "/");
        }
        return (basePath + "/" + name).replaceAll("/{2,}", "/");
    }

    public record CreateEntryRequest(String parentId, String name, String type, String content) {
    }

    public record UpdateEntryRequest(String newPath, String newName, Boolean extendPath) {
    }

    public record EntryResponse(
            String id,
            String name,
            boolean directory,
            long length,
            String modificationDate,
            String permissions,
            List<String> replicas) {
    }
}
