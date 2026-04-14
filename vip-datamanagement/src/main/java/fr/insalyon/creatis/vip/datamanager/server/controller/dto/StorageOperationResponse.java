package fr.insalyon.creatis.vip.datamanager.server.controller.dto;

import com.fasterxml.jackson.annotation.JsonView;

import fr.insalyon.creatis.vip.core.server.inter.DataViews;

public class StorageOperationResponse {

    @JsonView(DataViews.User.class)
    private String operationId;
    @JsonView(DataViews.User.class)
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
