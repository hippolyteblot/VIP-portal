package fr.insalyon.creatis.vip.datamanager.server.controller.dto;

import com.fasterxml.jackson.annotation.JsonView;

import fr.insalyon.creatis.vip.core.server.inter.DataViews;

public class StorageDownloadRequest {

    @JsonView(DataViews.User.class)
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
