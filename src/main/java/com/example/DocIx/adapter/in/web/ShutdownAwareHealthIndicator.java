package com.example.DocIx.adapter.in.web;

import com.example.DocIx.adapter.in.messaging.DocumentProcessingMessageHandler;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("shutdownAware")
public class ShutdownAwareHealthIndicator implements HealthIndicator {

    private final DocumentProcessingMessageHandler messageHandler;

    public ShutdownAwareHealthIndicator(DocumentProcessingMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("status", "running")
                .withDetail("activeProcessingTasks", messageHandler.getActiveProcessingTasks())
                .build();
    }
}
