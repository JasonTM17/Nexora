package com.nexora.platform.knowledge.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Bounded document extractors for the accepted formats: PDF, Markdown and
 * plain text. Parsers are CPU/time/page-bounded by the surrounding worker;
 * this class never opens a network connection and rejects empty output.
 */
public final class DocumentExtractors {
    private static final int MAX_PAGES = 2000;
    private static final int MAX_CHARACTERS = 5_000_000;

    private DocumentExtractors() {
    }

    public static Extraction extract(String contentType, InputStream bytes) {
        if (contentType == null || bytes == null) {
            throw new IllegalArgumentException("Extraction inputs are required.");
        }
        return switch (contentType) {
            case "text/plain", "text/markdown" -> extractText(bytes);
            case "application/pdf" -> extractPdf(bytes);
            default -> throw new IllegalArgumentException("Unsupported document content type: " + contentType);
        };
    }

    private static Extraction extractText(InputStream bytes) {
        String text = readAll(bytes);
        if (text.isBlank()) {
            throw new IllegalArgumentException("The document contains no extractable text.");
        }
        return bounded(text, 1, 1);
    }

    private static Extraction extractPdf(InputStream bytes) {
        try (PDDocument document = Loader.loadPDF(bytes.readAllBytes())) {
            int pages = document.getNumberOfPages();
            if (pages == 0) {
                throw new IllegalArgumentException("The PDF contains no pages.");
            }
            if (pages > MAX_PAGES) {
                throw new IllegalArgumentException("The PDF exceeds the page ceiling.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text.isBlank()) {
                throw new IllegalArgumentException("The PDF contains no extractable text.");
            }
            return bounded(text, 1, pages);
        } catch (IOException invalid) {
            throw new IllegalArgumentException("The PDF could not be parsed.", invalid);
        }
    }

    private static Extraction bounded(String text, int pageStart, int pageEnd) {
        if (text.length() > MAX_CHARACTERS) {
            throw new IllegalArgumentException("The document exceeds the character ceiling.");
        }
        return new Extraction(text, pageStart, pageEnd);
    }

    private static String readAll(InputStream bytes) {
        try {
            return new String(bytes.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException invalid) {
            throw new IllegalArgumentException("The document bytes could not be read.", invalid);
        }
    }

    public record Extraction(String text, int pageStart, int pageEnd) {
    }
}
