package com.example.DocIx.adapter.in.web;

import com.example.DocIx.domain.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex,
                                                                        HttpServletRequest request) {
        String safeMessage = LoggingUtil.maskSensitiveData(ex.getMessage());
        logger.warn("Validation error - Path: {}, Error: {}", request.getRequestURI(), safeMessage);

        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            ex.getMessage(),
            LocalDateTime.now()
        );

        LoggingUtil.logApiError(request.getMethod(), request.getRequestURI(), "unknown",
                              0, "Validation error: " + safeMessage);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
                                                                  HttpServletRequest request) {
        logger.warn("Method argument validation error - Path: {}, Fields: {}",
                   request.getRequestURI(), ex.getBindingResult().getFieldErrorCount());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, LoggingUtil.maskSensitiveData(errorMessage));
        });

        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed for request parameters",
            LocalDateTime.now(),
            fieldErrors
        );

        LoggingUtil.logApiError(request.getMethod(), request.getRequestURI(), "unknown",
                              0, "Validation failed: " + fieldErrors.size() + " field errors");

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
                                                                             HttpServletRequest request) {
        logger.error("File upload size exceeded - Path: {}, MaxSize: {}",
                    request.getRequestURI(), ex.getMaxUploadSize());

        ErrorResponse errorResponse = new ErrorResponse(
            "FILE_SIZE_EXCEEDED",
            "File size exceeds maximum allowed size of " + ex.getMaxUploadSize() + " bytes",
            LocalDateTime.now()
        );

        LoggingUtil.logApiError(request.getMethod(), request.getRequestURI(), "unknown",
                              0, "File size exceeded: " + ex.getMaxUploadSize() + " bytes");

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex,
                                                               HttpServletRequest request) {
        String safeMessage = LoggingUtil.maskSensitiveData(ex.getMessage());
        logger.error("Runtime exception - Path: {}, Error: {}",
                    request.getRequestURI(), safeMessage, ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An internal server error occurred",
            LocalDateTime.now()
        );

        LoggingUtil.logApiError(request.getMethod(), request.getRequestURI(), "unknown",
                              0, "Runtime exception: " + safeMessage);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
                                                               HttpServletRequest request) {
        String safeMessage = LoggingUtil.maskSensitiveData(ex.getMessage());
        logger.error("Unhandled exception - Path: {}, Type: {}, Error: {}",
                    request.getRequestURI(), ex.getClass().getSimpleName(), safeMessage, ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            LocalDateTime.now()
        );

        LoggingUtil.logApiError(request.getMethod(), request.getRequestURI(), "unknown",
                              0, "Unhandled exception: " + ex.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    public static class ErrorResponse {
        private String code;
        private String message;
        private LocalDateTime timestamp;
        private Map<String, String> details;

        public ErrorResponse(String code, String message, LocalDateTime timestamp) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
        }

        public ErrorResponse(String code, String message, LocalDateTime timestamp, Map<String, String> details) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
            this.details = details;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, String> getDetails() { return details; }
    }
}
