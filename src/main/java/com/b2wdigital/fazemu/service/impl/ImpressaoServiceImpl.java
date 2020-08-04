package com.b2wdigital.fazemu.service.impl;

import java.util.Map;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.service.ImpressaoService;
import com.b2wdigital.fazemu.exception.NotFoundException;
import com.google.common.collect.Maps;

/**
 * Impressao Service Impl.
 *
 * @author Marcelo Oliveira {marcelo.doliveira@b2wdigital.com}
 * @version 1.0
 */
@Service
public class ImpressaoServiceImpl implements ImpressaoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImpressaoServiceImpl.class);

    @Autowired
    private com.b2wdigital.fazemu.service.impl.PrintService printService;

    @Override
    public void imprimirPDF(String base64, String impressora) throws Exception {
        LOGGER.debug("ImpressaoServiceImpl: imprimirPDf");

        if (StringUtils.isBlank(base64)) {
            throw new NotFoundException("Arquivo nao informado.");
        }

        if (StringUtils.isBlank(impressora)) {
            throw new NotFoundException("Impressora nao informada.");
        }

        imprimir(base64, impressora);
    }

    protected void imprimir(String base64, String impressora) throws Exception {
        try {
            byte[] bytes = base64.getBytes();
            printService.print(bytes, impressora, "PDF");
        } catch (Exception e) {
            throw new NotFoundException("Nao foi possivel realizar a impressao a partir do arquivo informado - {}", e);
        }

    }

    @Override
    public Map<String, Boolean> listarImpressoras() {
        Boolean hasDefaultPrinter = false;
        PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
        if (defaultPrinter != null) {
            LOGGER.info("Default printer: " + defaultPrinter.getName());
            hasDefaultPrinter = true;
        }

        Map<String, Boolean> map = Maps.newHashMap();
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        if (printServices != null) {
            LOGGER.info("Number of print services: " + printServices.length);

            for (PrintService printer : printServices) {
                map.put(printer.getName(), (hasDefaultPrinter && defaultPrinter.getName().equals(printer.getName())));
            }

            return map;
        }

        throw new NotFoundException("Impressora nao localizada");
    }

}
