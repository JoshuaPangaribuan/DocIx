#!/bin/bash
# MinIO Bucket Initialization Script for DocIx

set -e

echo "Initializing MinIO bucket for DocIx..."

# Wait for MinIO to be ready
until curl -f http://localhost:9000/minio/health/live > /dev/null 2>&1; do
    echo "Waiting for MinIO to be ready..."
    sleep 2
done

# Install MinIO client if not present
if ! command -v mc &> /dev/null; then
    echo "Installing MinIO client..."
    curl -o mc https://dl.min.io/client/mc/release/linux-amd64/mc
    chmod +x mc
    sudo mv mc /usr/local/bin/
fi

# Configure MinIO client
mc alias set docix http://localhost:9000 minioadmin minioadmin

# Create bucket if it doesn't exist
if ! mc ls docix/docix-documents > /dev/null 2>&1; then
    echo "Creating docix-documents bucket..."
    mc mb docix/docix-documents
    echo "Bucket created successfully."
else
    echo "Bucket docix-documents already exists."
fi

# Set bucket policy for development (optional)
cat > /tmp/bucket-policy.json << EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "*"
            },
            "Action": [
                "s3:GetObject"
            ],
            "Resource": [
                "arn:aws:s3:::docix-documents/*"
            ]
        }
    ]
}
EOF

# Apply bucket policy (for development access)
mc policy set-json /tmp/bucket-policy.json docix/docix-documents

echo "MinIO bucket initialization completed."
echo "Bucket URL: http://localhost:9000/docix-documents"
echo "Console URL: http://localhost:9001"
echo "Credentials: minioadmin / minioadmin"
