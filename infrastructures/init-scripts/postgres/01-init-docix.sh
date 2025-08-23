#!/bin/bash
# DocIx PostgreSQL Database Initialization Script

set -e

echo "Initializing DocIx database..."

# Create additional schemas if needed
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create extensions
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";

    -- Create indexes for better performance
    CREATE INDEX IF NOT EXISTS idx_documents_content_search
    ON documents USING gin(extracted_content gin_trgm_ops);

    -- Create function for content search
    CREATE OR REPLACE FUNCTION search_documents(search_text TEXT)
    RETURNS TABLE(id VARCHAR, file_name VARCHAR, similarity REAL) AS
    \$\$
    BEGIN
        RETURN QUERY
        SELECT d.id, d.file_name,
               similarity(d.extracted_content, search_text) as sim
        FROM documents d
        WHERE d.extracted_content % search_text
        ORDER BY sim DESC;
    END;
    \$\$ LANGUAGE plpgsql;

    -- Grant permissions
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $POSTGRES_USER;

    -- Insert initial data if needed
    INSERT INTO documents (id, file_name, original_file_name, file_size, content_type, storage_path, uploader, uploaded_at, status)
    VALUES ('sample-doc-1', 'sample.pdf', 'sample.pdf', 1024, 'application/pdf', 'documents/sample.pdf', 'system', NOW(), 'UPLOADED')
    ON CONFLICT (id) DO NOTHING;

    -- Create stats view
    CREATE OR REPLACE VIEW document_stats AS
    SELECT
        status,
        COUNT(*) as count,
        AVG(file_size) as avg_size,
        SUM(file_size) as total_size
    FROM documents
    GROUP BY status;

EOSQL

echo "DocIx database initialization completed successfully!"
