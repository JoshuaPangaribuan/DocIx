-- Migration to replace extracted_content with download_url for PDF access
-- This change optimizes storage by storing MinIO PDF download URLs instead of large text content

-- Drop the extracted_content column and add download_url column
ALTER TABLE documents DROP COLUMN extracted_content;
ALTER TABLE documents ADD COLUMN download_url VARCHAR(1000);

-- Add index for download_url for better performance
CREATE INDEX idx_documents_download_url ON documents(download_url);
