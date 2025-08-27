package com.example.DocIx.adapter.out.extraction;

import com.example.DocIx.domain.port.out.PageExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementasi PageExtractor yang mengekstrak teks per-halaman secara akurat menggunakan PDFBox.
 * Fallback ke Tika jika terjadi kegagalan PDFBox.
 */
@Component
public class PageBasedContentExtractor implements PageExtractor {

    private final Tika tika = new Tika();

    @Override
    public List<DocumentPage> extractPages(InputStream fileContent, String fileName, String documentId)
            throws PageExtractionException {

        if (documentId == null || documentId.trim().isEmpty()) {
            throw new PageExtractionException("Document ID cannot be null or empty");
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new PageExtractionException("File name cannot be null or empty");
        }

        if (!fileName.toLowerCase().endsWith(".pdf")) {
            throw new PageExtractionException("File is not a PDF: " + fileName);
        }

        // PDFBox per-page extraction
        try (PDDocument document = PDDocument.load(fileContent)) {
            int numPages = document.getNumberOfPages();
            if (numPages <= 0) {
                throw new PageExtractionException("PDF has no pages: " + fileName);
            }

            List<DocumentPage> pages = new ArrayList<>(numPages);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(true);

            for (int pageIndex = 1; pageIndex <= numPages; pageIndex++) {
                stripper.setStartPage(pageIndex);
                stripper.setEndPage(pageIndex);
                String text = stripper.getText(document);
                String cleaned = text == null ? "" : text.trim();
                pages.add(new DocumentPage(documentId, pageIndex, cleaned));
            }

            return pages;
        } catch (IOException e) {
            // Fallback ke Tika sebagai cadangan
            try {
                String fullText = tika.parseToString(fileContent);
                if (fullText == null) {
                    throw new PageExtractionException("No text extracted from PDF: " + fileName);
                }
                String[] pageTexts = fullText.split("\f");
                List<DocumentPage> pages = new ArrayList<>();
                int pageNumber = 1;
                for (String pageText : pageTexts) {
                    String cleaned = pageText == null ? "" : pageText.trim();
                    pages.add(new DocumentPage(documentId, pageNumber++, cleaned));
                }
                if (pages.isEmpty()) {
                    throw new PageExtractionException("No pages extracted from PDF: " + fileName);
                }
                return pages;
            } catch (IOException | TikaException ex) {
                throw new PageExtractionException("Failed to extract pages from PDF: " + fileName, ex);
            }
        } catch (Exception e) {
            throw new PageExtractionException("Unexpected error while extracting pages: " + fileName, e);
        }
    }
}