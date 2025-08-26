package com.example.DocIx.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "indexing_page_log")
public class IndexingPageLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indexing_log_id", nullable = false)
    private IndexingLogJpaEntity indexingLog;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "page_status", nullable = false)
    private PageStatusEnum pageStatus = PageStatusEnum.PENDING;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public IndexingPageLogJpaEntity() {}

    public IndexingPageLogJpaEntity(IndexingLogJpaEntity indexingLog, int pageNumber) {
        this.indexingLog = indexingLog;
        this.pageNumber = pageNumber;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IndexingLogJpaEntity getIndexingLog() {
        return indexingLog;
    }

    public void setIndexingLog(IndexingLogJpaEntity indexingLog) {
        this.indexingLog = indexingLog;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public PageStatusEnum getPageStatus() {
        return pageStatus;
    }

    public void setPageStatus(PageStatusEnum pageStatus) {
        this.pageStatus = pageStatus;
    }

    public LocalDateTime getIndexedAt() {
        return indexedAt;
    }

    public void setIndexedAt(LocalDateTime indexedAt) {
        this.indexedAt = indexedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public enum PageStatusEnum {
        PENDING, INDEXED, FAILED
    }
}