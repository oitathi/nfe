package com.b2wdigital.fazemu.service.pdf;

import java.text.ParseException;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

/**
 *
 * @author dailton.almeida
 */
public class HeaderFooterDanfePageEvent extends PdfPageEventHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderFooterDanfePageEvent.class);
    private final DanfeServiceImpl danfeService;
    private final TNfeProc nfeProc;
    private final Boolean isNFeExterna;

    public HeaderFooterDanfePageEvent(DanfeServiceImpl danfeService, TNfeProc nfeProc, Boolean isNFeExterna) {
        this.danfeService = danfeService;
        this.nfeProc = nfeProc;
        this.isNFeExterna = isNFeExterna;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        try {
            danfeService.canhoto(writer, document, nfeProc.getNFe());
        } catch (DocumentException ex) {
            LOGGER.info(ex.getMessage(), ex);
        }
    }

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        try {
            danfeService.cabecalho(writer, document, nfeProc, isNFeExterna);
        } catch (Exception ex) {
            LOGGER.info(ex.getMessage(), ex);
            throw new FazemuServiceException(ex.getMessage());
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        try {
            danfeService.rodape(writer, document, nfeProc.getNFe());
        } catch (DocumentException ex) {
            LOGGER.info(ex.getMessage(), ex);
        } catch (ParseException ex) {
            java.util.logging.Logger.getLogger(HeaderFooterDanfePageEvent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
