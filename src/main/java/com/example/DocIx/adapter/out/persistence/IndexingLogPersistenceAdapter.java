package com.example.DocIx.adapter.out.persistence;

import com.example.DocIx.domain.model.IndexingLog;
import com.example.DocIx.domain.model.IndexingStatus;
import com.example.DocIx.domain.port.out.IndexingLogRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional
public class IndexingLogPersistenceAdapter implements IndexingLogRepository {

    private final IndexingLogJpaRepository jpaRepository;
    private final IndexingLogMapper mapper;

    public IndexingLogPersistenceAdapter(IndexingLogJpaRepository jpaRepository,
                                       IndexingLogMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public IndexingLog save(IndexingLog indexingLog) {
        IndexingLogJpaEntity entity = mapper.toJpaEntity(indexingLog);
        IndexingLogJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomainModel(savedEntity);
    }

    @Override
    public Optional<IndexingLog> findByDocumentId(String documentId) {
        return jpaRepository.findByDocumentId(documentId)
                .map(mapper::toDomainModel);
    }

    @Override
    public List<IndexingLog> findByStatus(IndexingStatus status) {
        IndexingLogJpaEntity.IndexingStatusEnum jpaStatus = mapToJpaStatus(status);
        return jpaRepository.findByIndexingStatus(jpaStatus)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<IndexingLog> findInProgressIndexing() {
        return jpaRepository.findInProgressIndexing()
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<IndexingLog> findFailedIndexingForRetry(int maxRetryCount) {
        return jpaRepository.findFailedIndexingForRetry(maxRetryCount)
                .stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByDocumentId(String documentId) {
        jpaRepository.deleteByDocumentId(documentId);
    }

    @Override
    public boolean existsByDocumentId(String documentId) {
        return jpaRepository.existsByDocumentId(documentId);
    }

    private IndexingLogJpaEntity.IndexingStatusEnum mapToJpaStatus(IndexingStatus status) {
        return switch (status) {
            case PENDING -> IndexingLogJpaEntity.IndexingStatusEnum.PENDING;
            case IN_PROGRESS -> IndexingLogJpaEntity.IndexingStatusEnum.IN_PROGRESS;
            case FULLY_INDEXED -> IndexingLogJpaEntity.IndexingStatusEnum.FULLY_INDEXED;
            case PARTIALLY_INDEXED -> IndexingLogJpaEntity.IndexingStatusEnum.PARTIALLY_INDEXED;
            case FAILED -> IndexingLogJpaEntity.IndexingStatusEnum.FAILED;
        };
    }
}
