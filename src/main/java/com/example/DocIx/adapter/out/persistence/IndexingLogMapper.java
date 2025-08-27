package com.example.DocIx.adapter.out.persistence;

import com.example.DocIx.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class IndexingLogMapper {

    public IndexingLogJpaEntity toJpaEntity(IndexingLog indexingLog) {
        if (indexingLog == null) return null;

        IndexingLogJpaEntity entity = new IndexingLogJpaEntity();
        entity.setId(indexingLog.getId());
        entity.setDocumentId(indexingLog.getDocumentId());
        entity.setTotalPages(indexingLog.getTotalPages());
        entity.setPagesIndexed(indexingLog.getPagesIndexed());
        entity.setPagesFailed(indexingLog.getPagesFailed());
        entity.setIndexingStatus(mapIndexingStatus(indexingLog.getIndexingStatus()));
        entity.setCreatedAt(indexingLog.getCreatedAt());
        entity.setUpdatedAt(indexingLog.getUpdatedAt());
        entity.setErrorDetails(indexingLog.getErrorDetails());

        // Map page logs dengan relasi yang benar menggunakan helper method
        if (indexingLog.getPageLogs() != null) {
            for (IndexingPageLog pageLog : indexingLog.getPageLogs()) {
                IndexingPageLogJpaEntity pageEntity = toPageJpaEntity(pageLog, entity);
                entity.addPageLog(pageEntity); // set relasi parent
            }
        }

        return entity;
    }

    public IndexingLog toDomainModel(IndexingLogJpaEntity entity) {
        if (entity == null) return null;

        IndexingLog indexingLog = new IndexingLog(
                entity.getId(),
                entity.getDocumentId(),
                entity.getTotalPages(),
                entity.getPagesIndexed(),
                entity.getPagesFailed(),
                mapIndexingStatus(entity.getIndexingStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getErrorDetails()
        );

        // Map page logs
        if (entity.getPageLogs() != null) {
            List<IndexingPageLog> pageLogs = entity.getPageLogs()
                    .stream()
                    .map(this::toPageDomainModel)
                    .collect(Collectors.toList());
            indexingLog.setPageLogs(pageLogs);
        }

        return indexingLog;
    }

    private IndexingPageLogJpaEntity toPageJpaEntity(IndexingPageLog pageLog, IndexingLogJpaEntity parentEntity) {
        if (pageLog == null) return null;

        IndexingPageLogJpaEntity entity = new IndexingPageLogJpaEntity();
        entity.setId(pageLog.getId());
        entity.setIndexingLog(parentEntity); // Set relasi parent agar FK terisi
        entity.setPageNumber(pageLog.getPageNumber());
        entity.setPageStatus(mapPageStatus(pageLog.getPageStatus()));
        entity.setIndexedAt(pageLog.getIndexedAt());
        entity.setErrorMessage(pageLog.getErrorMessage());
        entity.setRetryCount(pageLog.getRetryCount());
        entity.setCreatedAt(pageLog.getCreatedAt());

        return entity;
    }

    private IndexingPageLog toPageDomainModel(IndexingPageLogJpaEntity entity) {
        if (entity == null) return null;

        return new IndexingPageLog(
                entity.getId(),
                entity.getIndexingLog().getId(),
                entity.getPageNumber(),
                mapPageStatus(entity.getPageStatus()),
                entity.getIndexedAt(),
                entity.getErrorMessage(),
                entity.getRetryCount(),
                entity.getCreatedAt()
        );
    }

    private IndexingLogJpaEntity.IndexingStatusEnum mapIndexingStatus(IndexingStatus status) {
        if (status == null) return IndexingLogJpaEntity.IndexingStatusEnum.PENDING;

        return switch (status) {
            case PENDING -> IndexingLogJpaEntity.IndexingStatusEnum.PENDING;
            case IN_PROGRESS -> IndexingLogJpaEntity.IndexingStatusEnum.IN_PROGRESS;
            case FULLY_INDEXED -> IndexingLogJpaEntity.IndexingStatusEnum.FULLY_INDEXED;
            case PARTIALLY_INDEXED -> IndexingLogJpaEntity.IndexingStatusEnum.PARTIALLY_INDEXED;
            case FAILED -> IndexingLogJpaEntity.IndexingStatusEnum.FAILED;
        };
    }

    // Public method untuk digunakan oleh PersistenceAdapter
    public IndexingLogJpaEntity.IndexingStatusEnum mapIndexingStatusToJpa(IndexingStatus status) {
        return mapIndexingStatus(status);
    }

    private IndexingStatus mapIndexingStatus(IndexingLogJpaEntity.IndexingStatusEnum status) {
        if (status == null) return IndexingStatus.PENDING;

        return switch (status) {
            case PENDING -> IndexingStatus.PENDING;
            case IN_PROGRESS -> IndexingStatus.IN_PROGRESS;
            case FULLY_INDEXED -> IndexingStatus.FULLY_INDEXED;
            case PARTIALLY_INDEXED -> IndexingStatus.PARTIALLY_INDEXED;
            case FAILED -> IndexingStatus.FAILED;
        };
    }

    private IndexingPageLogJpaEntity.PageStatusEnum mapPageStatus(PageStatus status) {
        if (status == null) return IndexingPageLogJpaEntity.PageStatusEnum.PENDING;

        return switch (status) {
            case PENDING -> IndexingPageLogJpaEntity.PageStatusEnum.PENDING;
            case INDEXED -> IndexingPageLogJpaEntity.PageStatusEnum.INDEXED;
            case FAILED -> IndexingPageLogJpaEntity.PageStatusEnum.FAILED;
        };
    }

    private PageStatus mapPageStatus(IndexingPageLogJpaEntity.PageStatusEnum status) {
        if (status == null) return PageStatus.PENDING;

        return switch (status) {
            case PENDING -> PageStatus.PENDING;
            case INDEXED -> PageStatus.INDEXED;
            case FAILED -> PageStatus.FAILED;
        };
    }
}
