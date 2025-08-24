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
        entity.setTotalSegments(indexingLog.getTotalSegments());
        entity.setSegmentsIndexed(indexingLog.getSegmentsIndexed());
        entity.setSegmentsFailed(indexingLog.getSegmentsFailed());
        entity.setIndexingStatus(mapIndexingStatus(indexingLog.getIndexingStatus()));
        entity.setCreatedAt(indexingLog.getCreatedAt());
        entity.setUpdatedAt(indexingLog.getUpdatedAt());
        entity.setErrorDetails(indexingLog.getErrorDetails());

        // Map segment logs dengan relasi yang benar menggunakan helper method
        if (indexingLog.getSegmentLogs() != null) {
            for (IndexingSegmentLog segmentLog : indexingLog.getSegmentLogs()) {
                IndexingSegmentLogJpaEntity segmentEntity = toSegmentJpaEntity(segmentLog, null);
                entity.addSegmentLog(segmentEntity); // Menggunakan helper method
            }
        }

        return entity;
    }

    public IndexingLog toDomainModel(IndexingLogJpaEntity entity) {
        if (entity == null) return null;

        IndexingLog indexingLog = new IndexingLog(
                entity.getId(),
                entity.getDocumentId(),
                entity.getTotalSegments(),
                entity.getSegmentsIndexed(),
                entity.getSegmentsFailed(),
                mapIndexingStatus(entity.getIndexingStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getErrorDetails()
        );

        // Map segment logs
        if (entity.getSegmentLogs() != null) {
            List<IndexingSegmentLog> segmentLogs = entity.getSegmentLogs()
                    .stream()
                    .map(this::toSegmentDomainModel)
                    .collect(Collectors.toList());
            indexingLog.setSegmentLogs(segmentLogs);
        }

        return indexingLog;
    }

    private IndexingSegmentLogJpaEntity toSegmentJpaEntity(IndexingSegmentLog segmentLog, IndexingLogJpaEntity parentEntity) {
        if (segmentLog == null) return null;

        IndexingSegmentLogJpaEntity entity = new IndexingSegmentLogJpaEntity();
        entity.setId(segmentLog.getId());
        entity.setIndexingLog(parentEntity); // Set relasi parent
        entity.setSegmentNumber(segmentLog.getSegmentNumber());
        entity.setSegmentStatus(mapSegmentStatus(segmentLog.getSegmentStatus()));
        entity.setIndexedAt(segmentLog.getIndexedAt());
        entity.setErrorMessage(segmentLog.getErrorMessage());
        entity.setRetryCount(segmentLog.getRetryCount());
        entity.setCreatedAt(segmentLog.getCreatedAt());

        return entity;
    }

    private IndexingSegmentLog toSegmentDomainModel(IndexingSegmentLogJpaEntity entity) {
        if (entity == null) return null;

        return new IndexingSegmentLog(
                entity.getId(),
                entity.getIndexingLog().getId(),
                entity.getSegmentNumber(),
                mapSegmentStatus(entity.getSegmentStatus()),
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

    private IndexingSegmentLogJpaEntity.SegmentStatusEnum mapSegmentStatus(SegmentStatus status) {
        if (status == null) return IndexingSegmentLogJpaEntity.SegmentStatusEnum.PENDING;

        return switch (status) {
            case PENDING -> IndexingSegmentLogJpaEntity.SegmentStatusEnum.PENDING;
            case INDEXED -> IndexingSegmentLogJpaEntity.SegmentStatusEnum.INDEXED;
            case FAILED -> IndexingSegmentLogJpaEntity.SegmentStatusEnum.FAILED;
        };
    }

    private SegmentStatus mapSegmentStatus(IndexingSegmentLogJpaEntity.SegmentStatusEnum status) {
        if (status == null) return SegmentStatus.PENDING;

        return switch (status) {
            case PENDING -> SegmentStatus.PENDING;
            case INDEXED -> SegmentStatus.INDEXED;
            case FAILED -> SegmentStatus.FAILED;
        };
    }
}
