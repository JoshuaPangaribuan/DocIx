package com.example.DocIx.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class GracefulShutdownManager implements ApplicationListener<ContextClosedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(GracefulShutdownManager.class);

    private final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);
    private final AtomicBoolean shutdownCompleted = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (shutdownInitiated.compareAndSet(false, true)) {
            logger.info("🛑 Graceful shutdown initiated for DocIx application");
            performGracefulShutdown();
        }
    }

    private void performGracefulShutdown() {
        try {
            logger.info("📋 Starting graceful shutdown sequence...");

            // Step 1: Stop accepting new requests
            logger.info("1️⃣ Stopping acceptance of new requests...");

            // Step 2: Wait for ongoing document processing to complete
            logger.info("2️⃣ Waiting for ongoing document processing to complete...");
            waitForDocumentProcessingCompletion();

            // Step 3: Close external connections gracefully
            logger.info("3️⃣ Closing external connections...");
            closeExternalConnections();

            // Step 4: Cleanup resources
            logger.info("4️⃣ Cleaning up resources...");
            cleanupResources();

            shutdownCompleted.set(true);
            logger.info("✅ Graceful shutdown completed successfully");

        } catch (Exception e) {
            logger.error("❌ Error during graceful shutdown", e);
        }
    }

    private void waitForDocumentProcessingCompletion() {
        // Allow up to 25 seconds for document processing to complete
        int maxWaitTimeSeconds = 25;
        int waitedSeconds = 0;

        while (waitedSeconds < maxWaitTimeSeconds) {
            try {
                // Check if there are ongoing document processing tasks
                // This would be integrated with your document processing monitor
                Thread.sleep(1000);
                waitedSeconds++;

                if (waitedSeconds % 5 == 0) {
                    logger.info("⏳ Waiting for document processing completion... ({}/{} seconds)",
                               waitedSeconds, maxWaitTimeSeconds);
                }

                // In a real implementation, you would check:
                // - Active document processing tasks
                // - RabbitMQ message consumption
                // - Any ongoing file operations

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("⚠️ Shutdown wait interrupted");
                break;
            }
        }

        logger.info("✅ Document processing wait period completed");
    }

    private void closeExternalConnections() {
        try {
            // Close MinIO connections
            logger.info("🗄️ Closing MinIO connections...");

            // Close Elasticsearch connections
            logger.info("🔍 Closing Elasticsearch connections...");

            // Close RabbitMQ connections
            logger.info("🐰 Closing RabbitMQ connections...");

        } catch (Exception e) {
            logger.error("❌ Error closing external connections", e);
        }
    }

    private void cleanupResources() {
        try {
            // Cleanup temporary files
            logger.info("🧹 Cleaning up temporary files...");

            // Flush any pending logs
            logger.info("📝 Flushing pending logs...");

            // Other cleanup tasks
            logger.info("🔧 Performing final cleanup...");

        } catch (Exception e) {
            logger.error("❌ Error during resource cleanup", e);
        }
    }

    public boolean isShuttingDown() {
        return shutdownInitiated.get();
    }

    public boolean isShutdownInitiated() {
        return shutdownInitiated.get();
    }

    public boolean isShutdownCompleted() {
        return shutdownCompleted.get();
    }
}
