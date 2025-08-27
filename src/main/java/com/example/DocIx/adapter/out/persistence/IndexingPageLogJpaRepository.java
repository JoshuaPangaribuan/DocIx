package com.example.DocIx.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexingPageLogJpaRepository extends JpaRepository<IndexingPageLogJpaEntity, Long> {

    /**
     * Mencari page log berdasarkan indexing log ID dan nomor halaman
     */
    Optional<IndexingPageLogJpaEntity> findByIndexingLog_IdAndPageNumber(Long indexingLogId, int pageNumber);

    /**
     * Mencari semua page logs berdasarkan indexing log ID
     */
    List<IndexingPageLogJpaEntity> findByIndexingLog_Id(Long indexingLogId);

    /**
     * Mencari page logs berdasarkan status
     */
    List<IndexingPageLogJpaEntity> findByPageStatus(IndexingPageLogJpaEntity.PageStatusEnum pageStatus);

    /**
     * Mencari page logs yang gagal dan masih bisa di-retry
     */
    @Query("SELECT p FROM IndexingPageLogJpaEntity p WHERE p.pageStatus = 'FAILED' AND p.retryCount < :maxRetryCount")
    List<IndexingPageLogJpaEntity> findFailedPagesForRetry(@Param("maxRetryCount") int maxRetryCount);

    /**
     * Menghitung jumlah halaman berdasarkan status untuk indexing log tertentu
     */
    @Query("SELECT COUNT(p) FROM IndexingPageLogJpaEntity p WHERE p.indexingLog.id = :indexingLogId AND p.pageStatus = :status")
    long countByIndexingLogIdAndPageStatus(@Param("indexingLogId") Long indexingLogId, 
                                          @Param("status") IndexingPageLogJpaEntity.PageStatusEnum status);

    /**
     * Menghapus semua page logs berdasarkan indexing log ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM IndexingPageLogJpaEntity p WHERE p.indexingLog.id = :indexingLogId")
    void deleteByIndexingLogId(@Param("indexingLogId") Long indexingLogId);

    /**
     * Menghapus page logs berdasarkan document ID (melalui indexing log)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM IndexingPageLogJpaEntity p WHERE p.indexingLog.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") String documentId);

    /**
     * Mengecek apakah ada page logs untuk indexing log tertentu
     */
    boolean existsByIndexingLog_Id(Long indexingLogId);

    /**
     * Mencari page logs yang sedang dalam proses (IN_PROGRESS)
     */
    @Query("SELECT p FROM IndexingPageLogJpaEntity p WHERE p.indexingLog.indexingStatus = 'IN_PROGRESS'")
    List<IndexingPageLogJpaEntity> findInProgressPages();
}