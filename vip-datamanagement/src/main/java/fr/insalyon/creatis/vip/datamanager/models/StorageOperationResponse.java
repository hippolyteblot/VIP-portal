package fr.insalyon.creatis.vip.datamanager.models;

public class StorageOperationResponse {

    private String operationId;
    private String status;

    public StorageOperationResponse(String operationId, String status) {
        this.operationId = operationId;
        this.status = status;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getStatus() {
        return status;
    }
}
