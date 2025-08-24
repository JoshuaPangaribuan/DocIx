package com.example.DocIx.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "indexing_segment_log")
public class IndexingSegmentLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indexing_log_id", nullable = false)
    private IndexingLogJpaEntity indexingLog;

    @Column(name = "segment_number", nullable = false)
    private int segmentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment_status", nullable = false)
    private SegmentStatusEnum segmentStatus = SegmentStatusEnum.PENDING;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public IndexingSegmentLogJpaEntity() {}

    public IndexingSegmentLogJpaEntity(IndexingLogJpaEntity indexingLog, int segmentNumber) {
        this.indexingLog = indexingLog;
        this.segmentNumber = segmentNumber;
        this.createdAt = LocalDateTime.now();
    }

    // Getters dan Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public IndexingLogJpaEntity getIndexingLog() { return indexingLog; }
    public void setIndexingLog(IndexingLogJpaEntity indexingLog) { this.indexingLog = indexingLog; }

    public int getSegmentNumber() { return segmentNumber; }
    public void setSegmentNumber(int segmentNumber) { this.segmentNumber = segmentNumber; }

    public SegmentStatusEnum getSegmentStatus() { return segmentStatus; }
    public void setSegmentStatus(SegmentStatusEnum segmentStatus) { this.segmentStatus = segmentStatus; }

    public LocalDateTime getIndexedAt() { return indexedAt; }
    public void setIndexedAt(LocalDateTime indexedAt) { this.indexedAt = indexedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum SegmentStatusEnum {
        PENDING, INDEXED, FAILED
    }
}
