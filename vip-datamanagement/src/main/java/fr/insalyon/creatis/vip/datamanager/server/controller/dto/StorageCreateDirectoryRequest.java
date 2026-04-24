package fr.insalyon.creatis.vip.datamanager.server.controller.dto;

import com.fasterxml.jackson.annotation.JsonView;

import fr.insalyon.creatis.vip.core.server.inter.DataViews;

public class StorageCreateDirectoryRequest {

    @JsonView(DataViews.User.class)
    private String path;
    @JsonView(DataViews.User.class)
    private String name;

    public StorageCreateDirectoryRequest() {
    }

    public StorageCreateDirectoryRequest(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
