package com.example.DocIx.adapter.in.web;

import com.example.DocIx.adapter.in.messaging.DocumentProcessingMessageHandler;
import com.example.DocIx.config.GracefulShutdownManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("shutdownAware")
public class ShutdownAwareHealthIndicator implements HealthIndicator {

    private final GracefulShutdownManager shutdownManager;
    private final DocumentProcessingMessageHandler messageHandler;

    public ShutdownAwareHealthIndicator(GracefulShutdownManager shutdownManager,
                                      DocumentProcessingMessageHandler messageHandler) {
        this.shutdownManager = shutdownManager;
        this.messageHandler = messageHandler;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        if (shutdownManager.isShutdownCompleted()) {
            return builder
                    .down()
                    .withDetail("status", "shutdown_completed")
                    .withDetail("message", "Application has completed graceful shutdown")
                    .build();
        }

        if (shutdownManager.isShutdownInitiated()) {
            return builder
                    .outOfService()
                    .withDetail("status", "shutting_down")
                    .withDetail("message", "Application is shutting down gracefully")
                    .withDetail("activeProcessingTasks", messageHandler.getActiveProcessingTasks())
                    .build();
        }

        return builder
                .up()
                .withDetail("status", "running")
                .withDetail("message", "Application is running normally")
                .withDetail("activeProcessingTasks", messageHandler.getActiveProcessingTasks())
                .build();
    }
}
