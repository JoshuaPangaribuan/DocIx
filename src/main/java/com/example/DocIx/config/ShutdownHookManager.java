package com.example.DocIx.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class ShutdownHookManager {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownHookManager.class);

    private final ConfigurableApplicationContext applicationContext;
    private final GracefulShutdownManager shutdownManager;

    public ShutdownHookManager(ConfigurableApplicationContext applicationContext,
                              GracefulShutdownManager shutdownManager) {
        this.applicationContext = applicationContext;
        this.shutdownManager = shutdownManager;
    }

    @PostConstruct
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("🔄 Shutdown hook triggered - initiating graceful shutdown");

            if (!shutdownManager.isShutdownInitiated()) {
                try {
                    // Close the application context gracefully
                    applicationContext.close();
                    logger.info("✅ Application context closed successfully");
                } catch (Exception e) {
                    logger.error("❌ Error during application context shutdown", e);
                }
            }
        }, "DocIx-Shutdown-Hook"));

        logger.info("🎯 Shutdown hook registered successfully");
    }

    @PreDestroy
    public void cleanup() {
        logger.info("🧹 Performing final cleanup before shutdown");

        try {
            // Additional cleanup operations can be added here
            // For example: closing file handles, flushing caches, etc.

            logger.info("✅ Final cleanup completed");
        } catch (Exception e) {
            logger.error("❌ Error during final cleanup", e);
        }
    }
}
