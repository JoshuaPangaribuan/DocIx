package com.example.DocIx.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import jakarta.annotation.PreDestroy;

@Configuration
public class MinioConfig {

    private static final Logger logger = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private MinioClient minioClient;

    @Bean
    public MinioClient minioClient() {
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(minioUrl)
                    .credentials(accessKey, secretKey)
                    .build();

            logger.info("MinIO client configured successfully for endpoint: {}", minioUrl);
            return this.minioClient;
        } catch (Exception e) {
            logger.error("Failed to create MinIO client", e);
            throw new RuntimeException("Failed to create MinIO client", e);
        }
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeBucket() {
        try {
            MinioClient client = minioClient();

            boolean bucketExists = client.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!bucketExists) {
                client.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
                );
                logger.info("Created MinIO bucket: {}", bucketName);
            } else {
                logger.info("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            logger.warn("Failed to initialize MinIO bucket: {}. This might be due to MinIO server not being available. Error: {}",
                       bucketName, e.getMessage());
            // Don't throw exception to allow application to start even if MinIO is not available
            // The bucket will be created when first needed
        }
    }

    @PreDestroy
    @EventListener(ContextClosedEvent.class)
    public void closeMinioConnections() {
        try {
            if (minioClient != null) {
                logger.info("🗄️ Closing MinIO client connections during graceful shutdown");
                // MinIO client doesn't have explicit close method, but we can nullify the reference
                // The HTTP client connections will be closed by the underlying HTTP client
                minioClient = null;
                logger.info("✅ MinIO connections closed successfully");
            }
        } catch (Exception e) {
            logger.error("❌ Error closing MinIO connections", e);
        }
    }
}
