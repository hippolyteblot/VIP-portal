package fr.insalyon.creatis.vip.datamanager.server.controller.dto;

import com.fasterxml.jackson.annotation.JsonView;

import fr.insalyon.creatis.vip.core.server.inter.DataViews;

public class StorageCreateDirectoryRequest {

    @JsonView(DataViews.User.class)
    private String path;
    @JsonView(DataViews.User.class)
    private String name;

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
