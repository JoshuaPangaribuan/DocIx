package com.example.DocIx.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "indexing_log")
public class IndexingLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(name = "total_segments", nullable = false)
    private int totalSegments = 0;

    @Column(name = "segments_indexed", nullable = false)
    private int segmentsIndexed = 0;

    @Column(name = "segments_failed", nullable = false)
    private int segmentsFailed = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "indexing_status", nullable = false)
    private IndexingStatusEnum indexingStatus = IndexingStatusEnum.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @OneToMany(mappedBy = "indexingLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<IndexingSegmentLogJpaEntity> segmentLogs = new ArrayList<>();

    // Constructors
    public IndexingLogJpaEntity() {}

    public IndexingLogJpaEntity(String documentId) {
        this.documentId = documentId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Helper method untuk menambah segment log dengan relasi yang benar
    public void addSegmentLog(IndexingSegmentLogJpaEntity segmentLog) {
        segmentLogs.add(segmentLog);
        segmentLog.setIndexingLog(this);
    }

    public void removeSegmentLog(IndexingSegmentLogJpaEntity segmentLog) {
        segmentLogs.remove(segmentLog);
        segmentLog.setIndexingLog(null);
    }

    // Getters dan Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public int getTotalSegments() { return totalSegments; }
    public void setTotalSegments(int totalSegments) { this.totalSegments = totalSegments; }

    public int getSegmentsIndexed() { return segmentsIndexed; }
    public void setSegmentsIndexed(int segmentsIndexed) { this.segmentsIndexed = segmentsIndexed; }

    public int getSegmentsFailed() { return segmentsFailed; }
    public void setSegmentsFailed(int segmentsFailed) { this.segmentsFailed = segmentsFailed; }

    public IndexingStatusEnum getIndexingStatus() { return indexingStatus; }
    public void setIndexingStatus(IndexingStatusEnum indexingStatus) { this.indexingStatus = indexingStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }

    public List<IndexingSegmentLogJpaEntity> getSegmentLogs() { return segmentLogs; }
    public void setSegmentLogs(List<IndexingSegmentLogJpaEntity> segmentLogs) { this.segmentLogs = segmentLogs; }

    public enum IndexingStatusEnum {
        PENDING, IN_PROGRESS, FULLY_INDEXED, PARTIALLY_INDEXED, FAILED
    }
}
