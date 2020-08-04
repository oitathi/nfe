package com.b2wdigital.fazemu.presentation.web.service.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestParam;

import com.b2wdigital.fazemu.business.service.ConsultarProtocoloService;
import org.springframework.http.MediaType;

@RestController
public class ConsultarProtocoloRestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarProtocoloRestService.class);

    @Autowired
    private ConsultarProtocoloService consultarProtocoloService;

    @GetMapping(value = "/rest/atualizarConsultarProtocolo", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String atualizarConsultarProtocolo(@RequestParam(value = "chaveAcesso", required = true) String chaveAcesso,
            @RequestParam(value = "tipoServico", required = true) String tipoServico,
            @RequestParam(value = "isGerarNota", required = false) Boolean isGerarNota) throws Exception {
        LOGGER.info("atualizarConsultarProtocolo chaveAcesso {} tipoServico {} isGerarNota {}", chaveAcesso, tipoServico, isGerarNota);

        return consultarProtocoloService.atualizarConsultarProtocolo(chaveAcesso, tipoServico, isGerarNota);
    }

}
