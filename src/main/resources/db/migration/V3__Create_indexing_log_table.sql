-- Migration untuk membuat tabel tracking indexing status
CREATE TABLE indexing_log (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    total_segments INTEGER NOT NULL DEFAULT 0,
    segments_indexed INTEGER NOT NULL DEFAULT 0,
    segments_failed INTEGER NOT NULL DEFAULT 0,
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

-- Tabel untuk detail indexing per segmen
CREATE TABLE indexing_segment_log (
    id BIGSERIAL PRIMARY KEY,
    indexing_log_id BIGINT NOT NULL,
    segment_number INTEGER NOT NULL,
    segment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    indexed_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_segment_log_indexing_log
        FOREIGN KEY (indexing_log_id) REFERENCES indexing_log(id) ON DELETE CASCADE,
    CONSTRAINT chk_segment_status
        CHECK (segment_status IN ('PENDING', 'INDEXED', 'FAILED')),
    CONSTRAINT uk_indexing_segment
        UNIQUE (indexing_log_id, segment_number)
);

-- Index untuk performa query
CREATE INDEX idx_segment_log_indexing_id ON indexing_segment_log(indexing_log_id);
CREATE INDEX idx_segment_log_status ON indexing_segment_log(segment_status);
