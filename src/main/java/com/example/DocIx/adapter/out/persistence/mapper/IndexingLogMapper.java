package com.example.DocIx.adapter.out.persistence.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import com.example.DocIx.adapter.out.persistence.entity.IndexingLogJpaEntity;
import com.example.DocIx.adapter.out.persistence.entity.IndexingPageLogJpaEntity;
import com.example.DocIx.domain.model.IndexingLog;
import com.example.DocIx.domain.model.IndexingPageLog;
import com.example.DocIx.domain.model.IndexingStatus;
import com.example.DocIx.domain.model.PageStatus;

@Mapper(componentModel = "spring")
public interface IndexingLogMapper {

    @Mapping(source = "indexingStatus", target = "indexingStatus", qualifiedByName = "mapIndexingStatusToJpa")
    @Mapping(target = "pageLogs", ignore = true)
    IndexingLogJpaEntity toJpaEntity(IndexingLog indexingLog);

    @AfterMapping
    default void mapPageLogs(@MappingTarget IndexingLogJpaEntity entity, IndexingLog indexingLog) {
        if (indexingLog.getPageLogs() != null) {
            for (IndexingPageLog pageLog : indexingLog.getPageLogs()) {
                IndexingPageLogJpaEntity pageEntity = toPageJpaEntity(pageLog, entity);
                entity.addPageLog(pageEntity);
            }
        }
    }

    default IndexingLog toDomainEntity(IndexingLogJpaEntity jpaEntity) {
        if (jpaEntity == null)
            return null;

        return new IndexingLog(
                jpaEntity.getId(),
                jpaEntity.getDocumentId(),
                jpaEntity.getTotalPages(),
                jpaEntity.getPagesIndexed(),
                jpaEntity.getPagesFailed(),
                mapIndexingStatusFromJpa(jpaEntity.getIndexingStatus()),
                jpaEntity.getCreatedAt(),
                jpaEntity.getUpdatedAt(),
                jpaEntity.getErrorDetails());
    }

    @Mapping(source = "pageStatus", target = "pageStatus", qualifiedByName = "mapPageStatusToJpa")
    @Mapping(target = "indexingLog", ignore = true)
    IndexingPageLogJpaEntity toPageJpaEntity(IndexingPageLog pageLog);

    default IndexingPageLogJpaEntity toPageJpaEntity(IndexingPageLog pageLog, IndexingLogJpaEntity parentEntity) {
        if (pageLog == null)
            return null;

        IndexingPageLogJpaEntity entity = toPageJpaEntity(pageLog);
        entity.setIndexingLog(parentEntity);
        return entity;
    }

    @Named("mapPageStatusToJpa")
    default IndexingPageLogJpaEntity.PageStatusEnum mapPageStatusToJpa(PageStatus status) {
        if (status == null)
            return IndexingPageLogJpaEntity.PageStatusEnum.PENDING;
        return switch (status) {
            case PENDING -> IndexingPageLogJpaEntity.PageStatusEnum.PENDING;
            case INDEXED -> IndexingPageLogJpaEntity.PageStatusEnum.INDEXED;
            case FAILED -> IndexingPageLogJpaEntity.PageStatusEnum.FAILED;
        };
    }

    default IndexingPageLog toPageDomainEntity(IndexingPageLogJpaEntity entity) {
        if (entity == null)
            return null;

        return new IndexingPageLog(
                entity.getId(),
                entity.getIndexingLog() != null ? entity.getIndexingLog().getId() : null,
                entity.getPageNumber(),
                mapPageStatusFromJpa(entity.getPageStatus()),
                entity.getIndexedAt(),
                entity.getErrorMessage(),
                entity.getRetryCount(),
                entity.getCreatedAt());
    }

    @Named("mapIndexingStatusToJpa")
    default IndexingLogJpaEntity.IndexingStatusEnum mapIndexingStatusToJpa(IndexingStatus status) {
        if (status == null)
            return IndexingLogJpaEntity.IndexingStatusEnum.PENDING;

        return switch (status) {
            case PENDING -> IndexingLogJpaEntity.IndexingStatusEnum.PENDING;
            case IN_PROGRESS -> IndexingLogJpaEntity.IndexingStatusEnum.IN_PROGRESS;
            case FULLY_INDEXED -> IndexingLogJpaEntity.IndexingStatusEnum.FULLY_INDEXED;
            case PARTIALLY_INDEXED -> IndexingLogJpaEntity.IndexingStatusEnum.PARTIALLY_INDEXED;
            case FAILED -> IndexingLogJpaEntity.IndexingStatusEnum.FAILED;
        };
    }

    @Named("mapIndexingStatusFromJpa")
    default IndexingStatus mapIndexingStatusFromJpa(IndexingLogJpaEntity.IndexingStatusEnum status) {
        if (status == null)
            return IndexingStatus.PENDING;

        return switch (status) {
            case PENDING -> IndexingStatus.PENDING;
            case IN_PROGRESS -> IndexingStatus.IN_PROGRESS;
            case FULLY_INDEXED -> IndexingStatus.FULLY_INDEXED;
            case PARTIALLY_INDEXED -> IndexingStatus.PARTIALLY_INDEXED;
            case FAILED -> IndexingStatus.FAILED;
        };
    }

    @Named("mapPageStatusFromJpa")
    default PageStatus mapPageStatusFromJpa(IndexingPageLogJpaEntity.PageStatusEnum status) {
        if (status == null)
            return PageStatus.PENDING;

        return switch (status) {
            case PENDING -> PageStatus.PENDING;
            case INDEXED -> PageStatus.INDEXED;
            case FAILED -> PageStatus.FAILED;
        };
    }
}