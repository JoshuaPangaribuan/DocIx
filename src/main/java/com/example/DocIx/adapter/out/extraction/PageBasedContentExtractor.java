package com.example.DocIx.adapter.out.extraction;

import com.example.DocIx.domain.port.out.PageExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementasi PageExtractor menggunakan Apache PDFBox
 * untuk ekstraksi teks PDF per halaman
 */
@Component
public class PageBasedContentExtractor implements PageExtractor {

    @Override
    public List<DocumentPage> extractPages(InputStream fileContent, String fileName, String documentId)
            throws PageExtractionException {

        if (documentId == null || documentId.trim().isEmpty()) {
            throw new PageExtractionException("Document ID cannot be null or empty");
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new PageExtractionException("File name cannot be null or empty");
        }

        // Validasi bahwa file adalah PDF
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            throw new PageExtractionException("File is not a PDF: " + fileName);
        }

        List<DocumentPage> pages = new ArrayList<>();

        try (PDDocument document = PDDocument.load(fileContent)) {
            int totalPages = document.getNumberOfPages();

            if (totalPages == 0) {
                throw new PageExtractionException("PDF document has no pages: " + fileName);
            }

            PDFTextStripper textStripper = new PDFTextStripper();

            // Ekstrak teks untuk setiap halaman
            for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
                try {
                    // Set range halaman untuk ekstraksi (PDFBox menggunakan 1-based indexing)
                    textStripper.setStartPage(pageNumber);
                    textStripper.setEndPage(pageNumber);

                    // Ekstrak teks dari halaman
                    String pageText = textStripper.getText(document);

                    // Bersihkan teks (trim whitespace berlebih)
                    if (pageText != null) {
                        pageText = pageText.trim();
                    } else {
                        pageText = "";
                    }

                    // Buat DocumentPage object
                    DocumentPage documentPage = new DocumentPage(documentId, pageNumber, pageText);
                    pages.add(documentPage);

                } catch (IOException e) {
                    throw new PageExtractionException(
                            String.format("Error extracting text from page %d of file %s", pageNumber, fileName), e);
                }
            }

        } catch (IOException e) {
            throw new PageExtractionException("Error loading PDF document: " + fileName, e);
        } catch (Exception e) {
            throw new PageExtractionException("Unexpected error while processing PDF: " + fileName, e);
        }

        if (pages.isEmpty()) {
            throw new PageExtractionException("No pages extracted from PDF: " + fileName);
        }

        return pages;
    }
}