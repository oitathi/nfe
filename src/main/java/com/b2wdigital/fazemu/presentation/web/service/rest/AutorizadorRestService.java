package com.b2wdigital.fazemu.presentation.web.service.rest;

import com.b2wdigital.fazemu.business.repository.AutorizadorRepository;
import com.b2wdigital.fazemu.domain.Autorizador;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AutorizadorRestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutorizadorRestService.class);

    @Autowired
    private AutorizadorRepository autorizadorRepository;

    @GetMapping(value = "/autorizador", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<Autorizador> listAtivosByTipoDocumentoFiscal(String tipoDocumentoFiscal) {
        LOGGER.debug("AutorizadorRestService: listAtivosByTipoDocumentoFiscal");
        return autorizadorRepository.listAtivosByTipoDocumentoFiscal(tipoDocumentoFiscal);
    }

}
