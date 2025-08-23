package com.example.DocIx.adapter.in.web;

import com.example.DocIx.adapter.in.messaging.DocumentProcessingMessageHandler;
import com.example.DocIx.config.GracefulShutdownManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {

    private final GracefulShutdownManager shutdownManager;
    private final DocumentProcessingMessageHandler messageHandler;

    public SystemStatusController(GracefulShutdownManager shutdownManager,
                                DocumentProcessingMessageHandler messageHandler) {
        this.shutdownManager = shutdownManager;
        this.messageHandler = messageHandler;
    }

    @GetMapping("/status")
    public ResponseEntity<SystemStatusResponse> getSystemStatus() {
        SystemStatusResponse response = new SystemStatusResponse();
        response.setTimestamp(LocalDateTime.now());
        response.setActiveProcessingTasks(messageHandler.getActiveProcessingTasks());
        response.setShutdownInitiated(shutdownManager.isShutdownInitiated());
        response.setShutdownCompleted(shutdownManager.isShutdownCompleted());

        if (shutdownManager.isShutdownCompleted()) {
            response.setStatus("SHUTDOWN_COMPLETED");
            response.setMessage("Application has completed graceful shutdown");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        if (shutdownManager.isShutdownInitiated()) {
            response.setStatus("SHUTTING_DOWN");
            response.setMessage("Application is shutting down gracefully");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        response.setStatus("RUNNING");
        response.setMessage("Application is running normally");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> getReadinessStatus() {
        Map<String, Object> response = new HashMap<>();

        if (shutdownManager.isShutdownInitiated()) {
            response.put("ready", false);
            response.put("reason", "Application is shutting down");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        response.put("ready", true);
        response.put("activeProcessingTasks", messageHandler.getActiveProcessingTasks());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/live")
    public ResponseEntity<Map<String, Object>> getLivenessStatus() {
        Map<String, Object> response = new HashMap<>();

        if (shutdownManager.isShutdownCompleted()) {
            response.put("alive", false);
            response.put("reason", "Application has shutdown");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        response.put("alive", true);
        return ResponseEntity.ok(response);
    }

    public static class SystemStatusResponse {
        private LocalDateTime timestamp;
        private String status;
        private String message;
        private int activeProcessingTasks;
        private boolean shutdownInitiated;
        private boolean shutdownCompleted;

        // Getters and setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getActiveProcessingTasks() { return activeProcessingTasks; }
        public void setActiveProcessingTasks(int activeProcessingTasks) { this.activeProcessingTasks = activeProcessingTasks; }

        public boolean isShutdownInitiated() { return shutdownInitiated; }
        public void setShutdownInitiated(boolean shutdownInitiated) { this.shutdownInitiated = shutdownInitiated; }

        public boolean isShutdownCompleted() { return shutdownCompleted; }
        public void setShutdownCompleted(boolean shutdownCompleted) { this.shutdownCompleted = shutdownCompleted; }
    }
}
