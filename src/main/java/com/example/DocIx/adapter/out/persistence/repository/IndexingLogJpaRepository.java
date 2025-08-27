package com.example.DocIx.adapter.out.persistence.repository;

import com.example.DocIx.adapter.out.persistence.entity.IndexingLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexingLogJpaRepository extends JpaRepository<IndexingLogJpaEntity, Long> {

    Optional<IndexingLogJpaEntity> findByDocumentId(String documentId);

    List<IndexingLogJpaEntity> findByIndexingStatus(IndexingLogJpaEntity.IndexingStatusEnum status);

    @Query("SELECT i FROM IndexingLogJpaEntity i WHERE i.indexingStatus = 'IN_PROGRESS'")
    List<IndexingLogJpaEntity> findInProgressIndexing();

    @Query("SELECT i FROM IndexingLogJpaEntity i WHERE i.indexingStatus = 'FAILED' " +
           "AND i.pagesFailed < :maxRetryCount")
    List<IndexingLogJpaEntity> findFailedIndexingForRetry(@Param("maxRetryCount") int maxRetryCount);

    void deleteByDocumentId(String documentId);

    boolean existsByDocumentId(String documentId);
}