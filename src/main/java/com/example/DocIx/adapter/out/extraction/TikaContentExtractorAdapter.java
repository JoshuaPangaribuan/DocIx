package com.example.DocIx.adapter.out.extraction;

import com.example.DocIx.domain.port.out.ContentExtractor;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class TikaContentExtractorAdapter implements ContentExtractor {

    private final Tika tika;

    public TikaContentExtractorAdapter() {
        this.tika = new Tika();
    }

    @Override
    public String extractText(InputStream fileContent, String fileName) throws ContentExtractionException {
        try {
            String extractedText = tika.parseToString(fileContent);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new ContentExtractionException("No text content found in file: " + fileName, null);
            }

            return extractedText.trim();

        } catch (IOException e) {
            throw new ContentExtractionException("IO error while extracting content from file: " + fileName, e);
        } catch (TikaException e) {
            throw new ContentExtractionException("Tika parsing error for file: " + fileName, e);
        } catch (Exception e) {
            throw new ContentExtractionException("Unexpected error while extracting content from file: " + fileName, e);
        }
    }
}
