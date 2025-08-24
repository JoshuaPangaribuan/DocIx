package com.example.DocIx.domain.port.out;

import com.example.DocIx.domain.model.IndexingLog;
import com.example.DocIx.domain.model.IndexingStatus;

import java.util.List;
import java.util.Optional;

public interface IndexingLogRepository {

    IndexingLog save(IndexingLog indexingLog);

    Optional<IndexingLog> findByDocumentId(String documentId);

    List<IndexingLog> findByStatus(IndexingStatus status);

    List<IndexingLog> findInProgressIndexing();

    List<IndexingLog> findFailedIndexingForRetry(int maxRetryCount);

    void deleteByDocumentId(String documentId);

    boolean existsByDocumentId(String documentId);
}
