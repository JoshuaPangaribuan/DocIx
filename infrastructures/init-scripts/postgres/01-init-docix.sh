#!/bin/bash
# DocIx PostgreSQL Database Initialization Script

set -e

echo "Initializing DocIx database..."

# Create additional schemas if needed
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create extensions
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";

    -- Grant permissions to the application user
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO $POSTGRES_USER;

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

    -- Grant usage on schema public to the application user
    GRANT USAGE ON SCHEMA public TO $POSTGRES_USER;

    -- Alter default privileges in schema public
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $POSTGRES_USER;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $POSTGRES_USER;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO $POSTGRES_USER;

EOSQL

echo "DocIx database initialization completed."
