package com.example.DocIx.adapter.out.search;

/**
 * Exception khusus untuk error yang terjadi di search engine
 * dengan error code untuk mapping yang lebih baik ke HTTP response
 */
public class SearchEngineException extends RuntimeException {

    public enum ErrorCode {
        ES_UNAVAILABLE("Elasticsearch tidak tersedia"),
        TIMEOUT("Operasi timeout"),
        MAPPING_ERROR("Error pada mapping index"),
        QUERY_ERROR("Error pada query"),
        INDEX_ERROR("Error pada indexing"),
        UNKNOWN_ERROR("Error tidak diketahui");

        private final String description;

        ErrorCode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final ErrorCode errorCode;

    public SearchEngineException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
    }

    public SearchEngineException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public SearchEngineException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
