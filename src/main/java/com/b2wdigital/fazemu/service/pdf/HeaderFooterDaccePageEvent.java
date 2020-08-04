package com.b2wdigital.fazemu.service.pdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TProcEvento;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

public class HeaderFooterDaccePageEvent extends PdfPageEventHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderFooterDaccePageEvent.class);
    private final DacceServiceImpl dacceService;
    private final TProcEvento tProcEvento;
    private final TNfeProc nfeProc;

    public HeaderFooterDaccePageEvent(DacceServiceImpl dacceService, TProcEvento tProcEvento, TNfeProc nfeProc) {
        this.dacceService = dacceService;
        this.tProcEvento = tProcEvento;
        this.nfeProc = nfeProc;
    }

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        try {
            dacceService.cabecalho(writer, document, tProcEvento, nfeProc);
        } catch (Exception ex) {
            LOGGER.info(ex.getMessage(), ex);
            throw new FazemuServiceException(ex.getMessage());
        }
    }
}
