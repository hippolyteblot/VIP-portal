package fr.insalyon.creatis.vip.datamanager.models;

public class StorageDownloadRequest {

    private String path;

    public StorageDownloadRequest(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
