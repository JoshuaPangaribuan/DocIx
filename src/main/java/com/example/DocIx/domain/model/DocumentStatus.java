package com.example.DocIx.domain.model;

public enum DocumentStatus {
    UPLOADED,    // Document uploaded but not yet processed
    PROCESSING,  // Document is being processed (content extraction)
    PROCESSED,   // Document processed successfully and indexed
    FAILED       // Document processing failed
}
