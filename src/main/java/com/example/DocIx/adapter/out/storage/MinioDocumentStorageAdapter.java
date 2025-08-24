package com.example.DocIx.adapter.out.storage;

import com.example.DocIx.domain.port.out.DocumentStorage;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Component
public class MinioDocumentStorageAdapter implements DocumentStorage {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioDocumentStorageAdapter(MinioClient minioClient,
                                     @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @Override
    public String store(String fileName, InputStream fileContent, long fileSize, String contentType) {
        String objectName = "documents/" + fileName;
        String ct = contentType;
        if (ct == null || ct.trim().isEmpty()) {
            ct = (fileName != null && fileName.toLowerCase().endsWith(".pdf"))
                ? "application/pdf"
                : "application/octet-stream";
        }
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(fileContent, fileSize, -1)
                    .contentType(ct)
                    .build()
            );
            return objectName;
        } catch (Exception e) {
            throw new StorageException("Failed to store file: " + fileName, e);
        } finally {
            if (fileContent != null) {
                try { fileContent.close(); } catch (Exception ignore) {}
            }
        }
    }

    /**
     * Generate a presigned download URL for accessing the original PDF file
     * @param storagePath The storage path of the PDF file
     * @param expirationHours Number of hours the URL should remain valid
     * @return The presigned download URL for the PDF file
     */
    public String generateDownloadUrl(String storagePath, int expirationHours) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(storagePath)
                    .expiry(expirationHours, TimeUnit.HOURS)
                    .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to generate download URL for: " + storagePath, e);
        }
    }

    @Override
    public InputStream retrieve(String storagePath) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to retrieve file: " + storagePath, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to delete file: " + storagePath, e);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
