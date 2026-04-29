package fr.insalyon.creatis.vip.datamanager.models;

import fr.insalyon.creatis.vip.core.client.VipError;

/**
 * Error codes for storage operations.
 * 
 * STORAGE_VALIDATION_ERROR (4000): Invalid path structure or business constraint violation.
 *   Parameters: {0} = error details (e.g., "Path is not a direcPtory: /path/to/file")
 * 
 * STORAGE_PERMISSION_ERROR (4001): User lacks permission to perform the operation.
 *   Parameters: {0} = operation type (e.g., "read", "write", "delete")
 * 
 * STORAGE_NOT_FOUND_ERROR (4002): Path or resource does not exist.
 *   Parameters: {0} = path that was not found (e.g., "/vip/Home/missing/path")
 */
public enum DataManagementError implements VipError {

    STORAGE_VALIDATION_ERROR(4000, "Storage validation error: %s", 1, 400),
    STORAGE_PERMISSION_ERROR(4001, "Storage permission denied for %s operation", 1, 403),
    STORAGE_NOT_FOUND_ERROR(4002, "Storage resource not found: %s", 1, 404),
    OPERATION_NOT_READY(4003, "Operation is not ready yet", 0, 202);

    private final Integer code;
    private final String message;
    private final Integer expectedParams;
    private final Integer httpCode;

    DataManagementError(Integer code, String message, Integer expectedParams, Integer httpCode) {
        this.code = code;
        this.message = message;
        this.expectedParams = expectedParams;
        this.httpCode = httpCode;
    }

    @Override public Integer getCode() { return code; }
    @Override public String getMessage() { return message; }
    @Override public Integer getExpectedParameters() { return expectedParams; }
    @Override public Integer getHttpCode() { return httpCode; }
}
