package com.b2wdigital.fazemu.presentation.web.service.rest;

import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.exception.NotFoundException;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParametrosInfraRestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametrosInfraRestService.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();
    private static final String DEFAULT_MAPPING = "/parametrosInfra";

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @GetMapping(value = DEFAULT_MAPPING, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, String> getAllAsMap() {
        return parametrosInfraRepository.getAllAsMap();
    }

    @GetMapping(value = DEFAULT_MAPPING + "/{idParametro}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, String> getAsString(@PathVariable(value = "idParametro", required = true) String idParametro) {
        LOGGER.debug("getAsString idParametro {}", idParametro);
        String valor = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, idParametro);
        if (valor == null) {
            throw new NotFoundException("Par\u00E2metro '" + idParametro + "' n\u00E3o encontrado.");
        }
        return Collections.singletonMap(idParametro, valor);
    }

}
