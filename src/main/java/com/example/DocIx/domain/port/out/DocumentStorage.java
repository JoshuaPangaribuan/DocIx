package com.example.DocIx.domain.port.out;

import java.io.InputStream;

public interface DocumentStorage {
    String store(String fileName, InputStream fileContent, long fileSize, String contentType);
    InputStream retrieve(String storagePath);
    void delete(String storagePath);
    boolean exists(String storagePath);
}
