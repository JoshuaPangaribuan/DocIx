package com.example.DocIx.adapter.out.persistence;

import com.example.DocIx.adapter.out.persistence.entity.DocumentJpaEntity;
import com.example.DocIx.adapter.out.persistence.mapper.DocumentMapper;
import com.example.DocIx.adapter.out.persistence.repository.DocumentJpaRepository;
import com.example.DocIx.domain.model.Document;
import com.example.DocIx.domain.model.DocumentId;
import com.example.DocIx.domain.model.DocumentStatus;
import com.example.DocIx.domain.port.out.DocumentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DocumentPersistenceAdapter implements DocumentRepository {

    private final DocumentJpaRepository jpaRepository;
    private final DocumentMapper mapper;

    public DocumentPersistenceAdapter(DocumentJpaRepository jpaRepository, DocumentMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Document save(Document document) {
        DocumentJpaEntity jpaEntity = mapper.toJpaEntity(document);
        DocumentJpaEntity savedEntity = jpaRepository.save(jpaEntity);
        return mapper.toDomainEntity(savedEntity);
    }

    @Override
    public Optional<Document> findById(DocumentId id) {
        return jpaRepository.findById(id.getValue())
                .map(mapper::toDomainEntity);
    }

    @Override
    public List<Document> findByStatus(DocumentStatus status) {
        return jpaRepository.findByStatus(status)
                .stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    public List<Document> findByUploader(String uploader) {
        return jpaRepository.findByUploader(uploader)
                .stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    public List<Document> findByProcessed(boolean processed) {
        DocumentStatus status = processed ? DocumentStatus.PROCESSED : DocumentStatus.UPLOADED;
        return jpaRepository.findByStatus(status)
                .stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    public List<Document> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    public void deleteById(DocumentId id) {
        jpaRepository.deleteById(id.getValue());
    }

    @Override
    public boolean existsById(DocumentId id) {
        return jpaRepository.existsById(id.getValue());
    }
}
