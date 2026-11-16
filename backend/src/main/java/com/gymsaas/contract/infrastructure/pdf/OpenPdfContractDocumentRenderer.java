package com.gymsaas.contract.infrastructure.pdf;

import com.gymsaas.contract.domain.ContractDocumentRenderer;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;

/** Renders the plain-text contract into an A4 PDF (Helvetica / WinAnsi covers Portuguese). */
@Component
public class OpenPdfContractDocumentRenderer implements ContractDocumentRenderer {

    private static final Pattern SECTION = Pattern.compile("^\\d+\\. [\\p{Lu} ]+$");

    @Override
    public String contentType() {
        return "application/pdf";
    }

    @Override
    public byte[] render(String title, String content) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 56, 56, 56, 56);
        try {
            PdfWriter.getInstance(document, output);
            document.addTitle(title);
            document.addCreator("GymSaaS");
            document.open();
            Font titleFont = new Font(Font.HELVETICA, 13, Font.BOLD);
            Font sectionFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            String[] lines = content.split("\n", -1);
            for (int index = 0; index < lines.length; index++) {
                String line = lines[index];
                if (line.isBlank()) {
                    document.add(new Paragraph(Chunk.NEWLINE));
                } else if (index == 0) {
                    document.add(new Paragraph(line, titleFont));
                } else if (SECTION.matcher(line).matches()) {
                    document.add(new Paragraph(line, sectionFont));
                } else {
                    document.add(new Paragraph(line, bodyFont));
                }
            }
            document.close();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Could not render contract PDF", exception);
        }
        return output.toByteArray();
    }
}
