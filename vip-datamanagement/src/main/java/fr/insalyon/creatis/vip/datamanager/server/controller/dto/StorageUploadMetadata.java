package fr.insalyon.creatis.vip.datamanager.server.controller.dto;

import com.fasterxml.jackson.annotation.JsonView;

import fr.insalyon.creatis.vip.core.server.inter.DataViews;

public class StorageUploadMetadata {

    @JsonView(DataViews.User.class)
    private String path;
    @JsonView(DataViews.User.class)
    private String fileName;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
