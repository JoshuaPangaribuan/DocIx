-- Initial migration for DocIx document indexing system
-- Creates documents table to store document metadata

CREATE TABLE documents (
    id VARCHAR(36) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    uploader VARCHAR(100) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    extracted_content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    error_message TEXT,
    last_processed_at TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('UPLOADED', 'PROCESSING', 'PROCESSED', 'FAILED'))
);

-- Indexes for better performance
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_uploader ON documents(uploader);
CREATE INDEX idx_documents_uploaded_at ON documents(uploaded_at);
CREATE INDEX idx_documents_content_type ON documents(content_type);
