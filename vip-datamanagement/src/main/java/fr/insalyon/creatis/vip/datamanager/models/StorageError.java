package fr.insalyon.creatis.vip.datamanager.models;

import fr.insalyon.creatis.vip.core.client.VipError;

/**
 * Error codes for storage operations (code range 9xxx for legacy compatibility).
 * STORAGE_VALIDATION_ERROR (400): invalid path structure or business constraint violation.
 * STORAGE_PERMISSION_ERROR (403): user lacks permission to perform the operation.
 */
public enum StorageError implements VipError {

    STORAGE_VALIDATION_ERROR(9999, "Storage validation error (Error code 9999)", 0, 400),
    STORAGE_PERMISSION_ERROR(9999, "Storage permission denied (Error code 9999)", 0, 403),
    STORAGE_NOT_FOUND_ERROR(9999, "Storage resource not found (Error code 9999)", 0, 404);

    private final Integer code;
    private final String message;
    private final Integer expectedParams;
    private final Integer httpCode;

    StorageError(Integer code, String message, Integer expectedParams, Integer httpCode) {
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
