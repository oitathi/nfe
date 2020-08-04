package com.b2wdigital.fazemu.service.pdf;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.util.ISO8601Utils;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

@Service
public abstract class AbstractPdfService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPdfService.class);
    
    private static final String STR_ZERO = "0.00";

    // builders auxiliares
    protected PdfPTable buildTable(int[] relativeWidths) throws DocumentException {
        PdfPTable table = new PdfPTable(relativeWidths.length);
        table.setWidthPercentage(100f);
        table.setWidths(relativeWidths);
        return table;
    }
    
    protected Paragraph defaultParagraph(String s, Font font) {
        Paragraph p = new Paragraph(s, font);
        return p;
    }
    
    protected Paragraph centeredParagraph(String s, Font font) {
        Paragraph p = new Paragraph(s, font);
        p.setAlignment(Paragraph.ALIGN_CENTER);
        return p;
    }

    //celulas sem titulo; exemplo do uso: linhas da nfe, titulo das secoes
    protected void addCell(PdfPTable table, Font font, String... texts) {
        for (String text: texts) {
            addCell(table, text, font);
        }
    }
    
    protected PdfPCell addCell(PdfPTable table, String text, Font font) {
        return this.addCell(table, text, font, Paragraph.ALIGN_CENTER);
    }
    
    protected PdfPCell addCell(PdfPTable table, String text, Font font, int contentAlignment) {
        return this.addCell(table, text, font, contentAlignment, 1);
    }
    
    protected PdfPCell addCell(PdfPTable table, String text, Font font, int contentAlignment, int colspan) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(contentAlignment);
        PdfPCell cell = new PdfPCell();
        cell.addElement(p);
        if (colspan > 1) {
            cell.setColspan(colspan);
        }
        return table.addCell(cell);
    }
    
    //celulas com titulo
    protected PdfPCell addCell(PdfPTable table, String title, Font titleFont, String content, Font contentFont) {
        return this.addCell(table, title, titleFont, content, contentFont, Paragraph.ALIGN_CENTER, 1);
    }
    
    protected PdfPCell addCell(PdfPTable table, String title, Font titleFont, String content, Font contentFont, int contentAlignment) {
        return this.addCell(table, title, titleFont, content, contentFont, contentAlignment, 1);
    }
    
    protected PdfPCell addCell(PdfPTable table, String title, Font titleFont, String content, Font contentFont, int contentAlignment, int colspan) {
        Paragraph pTitle = new Paragraph(title, titleFont);
        pTitle.setAlignment(Paragraph.ALIGN_LEFT);
        pTitle.setSpacingBefore(0f);
        pTitle.setSpacingAfter(0f);

        Paragraph pContent = new Paragraph(content, contentFont);
        pContent.setAlignment(contentAlignment);
        pContent.setSpacingBefore(0f);
        pContent.setSpacingAfter(0f);

        PdfPCell cell = new PdfPCell();
        cell.addElement(pTitle);
        cell.addElement(pContent);
        cell.setPaddingTop(0f);
        if (colspan > 1) {
            cell.setColspan(colspan);
        }
        return table.addCell(cell);
    }
    
    protected String formatDateTime(String datetime, String outputFormat) {
        try {
            Date d = ISO8601Utils.parse(datetime, new ParsePosition(0));
            FastDateFormat df = FastDateFormat.getInstance(outputFormat);
            return df.format(d);
        } catch (ParseException ex) {
            LOGGER.error(ex.getMessage(), ex);
            return StringUtils.EMPTY;
        }
    }

    protected String snvl(String s) {
        return StringUtils.defaultString(s);
    }
    
    protected String cnvl(String s) {
        return StringUtils.defaultIfBlank(s, StringUtils.LF);
    }
    
    protected String vnvl(String s) {
        return StringUtils.defaultIfBlank(s, STR_ZERO);
    }
    
}
