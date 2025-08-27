package com.example.DocIx.adapter.out.persistence.entity;

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

    @Column(name = "total_pages", nullable = false)
    private int totalPages = 0;

    @Column(name = "pages_indexed", nullable = false)
    private int pagesIndexed = 0;

    @Column(name = "pages_failed", nullable = false)
    private int pagesFailed = 0;

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
    private List<IndexingPageLogJpaEntity> pageLogs = new ArrayList<>();

    // Constructors
    public IndexingLogJpaEntity() {}

    public IndexingLogJpaEntity(String documentId) {
        this.documentId = documentId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Helper method untuk menambah page log dengan relasi yang benar
    public void addPageLog(IndexingPageLogJpaEntity pageLog) {
        pageLogs.add(pageLog);
        pageLog.setIndexingLog(this);
    }

    public void removePageLog(IndexingPageLogJpaEntity pageLog) {
        pageLogs.remove(pageLog);
        pageLog.setIndexingLog(null);
    }

    // Getters dan Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getPagesIndexed() { return pagesIndexed; }
    public void setPagesIndexed(int pagesIndexed) { this.pagesIndexed = pagesIndexed; }

    public int getPagesFailed() { return pagesFailed; }
    public void setPagesFailed(int pagesFailed) { this.pagesFailed = pagesFailed; }

    public IndexingStatusEnum getIndexingStatus() { return indexingStatus; }
    public void setIndexingStatus(IndexingStatusEnum indexingStatus) { this.indexingStatus = indexingStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }

    public List<IndexingPageLogJpaEntity> getPageLogs() { return pageLogs; }
    public void setPageLogs(List<IndexingPageLogJpaEntity> pageLogs) { this.pageLogs = pageLogs; }

    public enum IndexingStatusEnum {
        PENDING, IN_PROGRESS, FULLY_INDEXED, PARTIALLY_INDEXED, FAILED
    }
}