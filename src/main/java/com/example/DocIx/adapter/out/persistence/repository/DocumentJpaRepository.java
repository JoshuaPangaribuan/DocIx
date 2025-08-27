package com.example.DocIx.adapter.out.persistence.repository;

import com.example.DocIx.adapter.out.persistence.entity.DocumentJpaEntity;
import com.example.DocIx.domain.model.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentJpaEntity, String> {
    List<DocumentJpaEntity> findByStatus(DocumentStatus status);
    List<DocumentJpaEntity> findByUploader(String uploader);
}