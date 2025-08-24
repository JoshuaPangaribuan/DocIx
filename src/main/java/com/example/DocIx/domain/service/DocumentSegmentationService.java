package com.example.DocIx.domain.service;

import com.example.DocIx.domain.model.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentSegmentationService {

    private final int segmentSize;

    // Pattern to find sentence boundaries for better segmentation
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("[.!?]+\\s+");

    public DocumentSegmentationService(@Value("${docix.document.segment.size:500}") int segmentSize) {
        this.segmentSize = segmentSize;
    }

    /**
     * Segments the extracted text into chunks of configurable size.
     * Attempts to break at sentence boundaries when possible to maintain readability.
     *
     * @param extractedText The full text content to segment
     * @return List of text segments
     */
    public List<String> segmentText(String extractedText) {
        if (extractedText == null || extractedText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> segments = new ArrayList<>();
        String text = extractedText.trim();

        // If the text is smaller than segment size, return as single segment
        if (text.length() <= segmentSize) {
            segments.add(text);
            return segments;
        }

        int currentPosition = 0;

        while (currentPosition < text.length()) {
            int endPosition = Math.min(currentPosition + segmentSize, text.length());

            // If this is not the last segment, try to find a good breaking point
            if (endPosition < text.length()) {
                endPosition = findOptimalBreakPoint(text, currentPosition, endPosition);
            }

            String segment = text.substring(currentPosition, endPosition).trim();
            if (!segment.isEmpty()) {
                segments.add(segment);
            }

            currentPosition = endPosition;
        }

        return segments;
    }

    /**
     * Finds the optimal break point for text segmentation.
     * Prioritizes sentence boundaries, then word boundaries.
     */
    private int findOptimalBreakPoint(String text, int startPosition, int maxEndPosition) {
        // Look for sentence boundaries within a reasonable range before the max position
        int searchStart = Math.max(startPosition, maxEndPosition - 100);
        String searchText = text.substring(searchStart, maxEndPosition);

        Matcher matcher = SENTENCE_BOUNDARY.matcher(searchText);
        int lastSentenceEnd = -1;

        while (matcher.find()) {
            lastSentenceEnd = searchStart + matcher.end();
        }

        // If we found a sentence boundary, use it
        if (lastSentenceEnd > startPosition) {
            return lastSentenceEnd;
        }

        // Otherwise, try to break at a word boundary
        for (int i = maxEndPosition - 1; i > startPosition; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }

        // If no good break point found, use the max position
        return maxEndPosition;
    }

    /**
     * Segments a document into DocumentSegment objects with metadata
     */
    public List<DocumentSegment> segmentDocument(String extractedText, Document document) {
        List<String> textSegments = segmentText(extractedText);
        List<DocumentSegment> segments = new ArrayList<>();

        for (int i = 0; i < textSegments.size(); i++) {
            DocumentSegment segment = new DocumentSegment(
                    document.getId().getValue(),
                    document.getFileName(),
                    document.getOriginalFileName(),
                    textSegments.get(i),
                    i + 1, // segment number (1-based)
                    textSegments.size(), // total segments
                    document.getUploader(),
                    document.getUploadedAt().toString(),
                    "/api/documents/download/" + document.getId().getValue()
            );
            segments.add(segment);
        }

        return segments;
    }

    public int getSegmentSize() {
        return segmentSize;
    }

    /**
     * Inner class representing a document segment with metadata
     */
    public static class DocumentSegment {
        private String documentId;
        private String fileName;
        private String originalFileName;
        private String content;
        private int segmentNumber;
        private int totalSegments;
        private String uploader;
        private String uploadedAt;
        private String downloadUrl;

        public DocumentSegment() {}

        public DocumentSegment(String documentId, String fileName, String originalFileName,
                             String content, int segmentNumber, int totalSegments,
                             String uploader, String uploadedAt, String downloadUrl) {
            this.documentId = documentId;
            this.fileName = fileName;
            this.originalFileName = originalFileName;
            this.content = content;
            this.segmentNumber = segmentNumber;
            this.totalSegments = totalSegments;
            this.uploader = uploader;
            this.uploadedAt = uploadedAt;
            this.downloadUrl = downloadUrl;
        }

        // Getters and setters
        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getOriginalFileName() { return originalFileName; }
        public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public int getSegmentNumber() { return segmentNumber; }
        public void setSegmentNumber(int segmentNumber) { this.segmentNumber = segmentNumber; }

        public int getTotalSegments() { return totalSegments; }
        public void setTotalSegments(int totalSegments) { this.totalSegments = totalSegments; }

        public String getUploader() { return uploader; }
        public void setUploader(String uploader) { this.uploader = uploader; }

        public String getUploadedAt() { return uploadedAt; }
        public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }

        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    }
}
