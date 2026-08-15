package com.nexora.platform.knowledge.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

class DocumentExtractorsTest {

    @Test
    void extractsPlainTextWithinBounds() {
        String text = "A plain text document.\nWith two lines.";
        DocumentExtractors.Extraction extraction = DocumentExtractors.extract(
                "text/plain", new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        assertThat(extraction.text()).contains("plain text document");
        assertThat(extraction.pageStart()).isEqualTo(1);
        assertThat(extraction.pageEnd()).isEqualTo(1);
    }

    @Test
    void extractsPdfTextWithPageRange() throws IOException {
        byte[] pdf = singlePagePdf("Publishing immutable page versions");
        DocumentExtractors.Extraction extraction = DocumentExtractors.extract(
                "application/pdf", new ByteArrayInputStream(pdf));
        assertThat(extraction.text()).contains("Publishing immutable page versions");
        assertThat(extraction.pageStart()).isEqualTo(1);
        assertThat(extraction.pageEnd()).isEqualTo(1);
    }

    @Test
    void rejectsEmptyDocuments() {
        assertThatThrownBy(() -> DocumentExtractors.extract(
                "text/plain", new ByteArrayInputStream("   ".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsCorruptPdf() {
        assertThatThrownBy(() -> DocumentExtractors.extract(
                "application/pdf", new ByteArrayInputStream("not-a-pdf".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnsupportedContentTypes() {
        assertThatThrownBy(() -> DocumentExtractors.extract(
                "application/zip", new ByteArrayInputStream(new byte[]{1, 2, 3})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported");
    }

    private byte[] singlePagePdf(String content) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(content);
                stream.endText();
            }
            var output = new java.io.ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }
}
