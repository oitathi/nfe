package com.b2wdigital.fazemu.presentation.web.controller;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.service.DacceService;
import com.b2wdigital.fazemu.exception.NotFoundException;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class DacceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DacceController.class);

    @Autowired
    private DacceService dacceService;

    @RequestMapping(value = "/nfe/{chaveAcesso}/dacce", method = RequestMethod.GET, produces = MediaType.APPLICATION_PDF_VALUE)
    public byte[] getDACCE(HttpServletResponse response,
            @PathVariable("chaveAcesso") String chaveAcesso,
            @RequestHeader(value = "Content-Transfer-Encoding", required = false) String contentTransferEncoding)
            throws Exception {
        LOGGER.debug("DacceController: getDACCE");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        com.itextpdf.text.Document document = dacceService.fromChaveAcessoToPDF(chaveAcesso, bos);

        if (document == null) {
            throw new NotFoundException("Chave " + chaveAcesso + " n\u00E3o encontrada.");
        }

        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("application/pdf");

        try {
            if (StringUtils.equalsIgnoreCase(contentTransferEncoding, "base64")) {
                IOUtils.write(Base64.getEncoder().encode(bos.toByteArray()), response.getOutputStream());
                return Base64.getEncoder().encode(bos.toByteArray());
            } else {
                IOUtils.write(bos.toByteArray(), response.getOutputStream());
                return bos.toByteArray();
            }
        } finally {
            response.getOutputStream().flush();
            response.getOutputStream().close();

            bos.flush();
            bos.close();
        }
    }

}
