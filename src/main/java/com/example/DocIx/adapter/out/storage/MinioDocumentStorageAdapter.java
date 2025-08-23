package com.example.DocIx.adapter.out.storage;

import com.example.DocIx.domain.port.out.DocumentStorage;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

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
        try {
            String objectName = "documents/" + fileName;

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(fileContent, fileSize, -1)
                    .contentType(contentType)
                    .build()
            );

            return objectName;
        } catch (Exception e) {
            throw new StorageException("Failed to store file: " + fileName, e);
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
