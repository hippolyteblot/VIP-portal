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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.datamanager.client.DataManagerConstants;
import fr.insalyon.creatis.vip.datamanager.models.Data;
import fr.insalyon.creatis.vip.datamanager.server.business.StorageBusiness;

@RestController
@RequestMapping("/storage")
public class StorageController {

    private final StorageBusiness storageBusiness;
    @Autowired
    public StorageController(StorageBusiness storageBusiness) {
        this.storageBusiness = storageBusiness;
    }

    // GET /storage/<path>?refresh=<bool>
    @GetMapping(value = "/**")
    public List<Data> listStoragePath(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") boolean refresh) throws VipException {
        String resolvedPath = resolvePath(request);
        List<Data> children = storageBusiness.listDir(resolvedPath);
        return children;
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
}
