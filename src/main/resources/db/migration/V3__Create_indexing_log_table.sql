-- Migration untuk membuat tabel tracking indexing status
CREATE TABLE indexing_log (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    total_pages INTEGER NOT NULL DEFAULT 0,
    pages_indexed INTEGER NOT NULL DEFAULT 0,
    pages_failed INTEGER NOT NULL DEFAULT 0,
    indexing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_details TEXT,

    CONSTRAINT fk_indexing_log_document
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT chk_indexing_status
        CHECK (indexing_status IN ('PENDING', 'IN_PROGRESS', 'FULLY_INDEXED', 'PARTIALLY_INDEXED', 'FAILED'))
);

-- Index untuk performa query
CREATE INDEX idx_indexing_log_document_id ON indexing_log(document_id);
CREATE INDEX idx_indexing_log_status ON indexing_log(indexing_status);
CREATE INDEX idx_indexing_log_created_at ON indexing_log(created_at);

-- Tabel untuk detail indexing per page
CREATE TABLE indexing_page_log (
    id BIGSERIAL PRIMARY KEY,
    indexing_log_id BIGINT NOT NULL,
    page_number INTEGER NOT NULL,
    page_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    indexed_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_page_log_indexing_log
        FOREIGN KEY (indexing_log_id) REFERENCES indexing_log(id) ON DELETE CASCADE,
    CONSTRAINT chk_page_status
        CHECK (page_status IN ('PENDING', 'INDEXED', 'FAILED')),
    CONSTRAINT uk_indexing_page
        UNIQUE (indexing_log_id, page_number)
);

-- Index untuk performa query
CREATE INDEX idx_page_log_indexing_id ON indexing_page_log(indexing_log_id);
CREATE INDEX idx_page_log_status ON indexing_page_log(page_status);
