package com.example.DocIx.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IndexingSegmentLogJpaRepository extends JpaRepository<IndexingSegmentLogJpaEntity, Long> {

    @Modifying
    @Query("DELETE FROM IndexingSegmentLogJpaEntity s WHERE s.indexingLog.id = :indexingLogId")
    void deleteByIndexingLogId(@Param("indexingLogId") Long indexingLogId);

    @Query("SELECT s FROM IndexingSegmentLogJpaEntity s WHERE s.indexingLog.id = :indexingLogId AND s.segmentNumber = :segmentNumber")
    Optional<IndexingSegmentLogJpaEntity> findByIndexingLogIdAndSegmentNumber(@Param("indexingLogId") Long indexingLogId, @Param("segmentNumber") int segmentNumber);
}
