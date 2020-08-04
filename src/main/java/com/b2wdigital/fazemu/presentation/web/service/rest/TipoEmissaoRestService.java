package com.b2wdigital.fazemu.presentation.web.service.rest;

import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.repository.TipoEmissaoRepository;
import com.b2wdigital.fazemu.domain.TipoEmissao;
import com.b2wdigital.fazemu.exception.NotFoundException;
import com.b2wdigital.fazemu.utils.FazemuUtils;

@RestController
public class TipoEmissaoRestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TipoEmissaoRestService.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;
    @Autowired
    private TipoEmissaoRepository tipoEmissaoRepository;

    @GetMapping(value = "/tipoEmissao", params = "!idEstado", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<TipoEmissao> listAtivos() {
        return tipoEmissaoRepository.listAtivos();
    }

    @GetMapping(value = "/tipoEmissao", params = "idEstado", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public TipoEmissao findByIdEstado(@RequestParam(value = "idEstado", required = true) Long idEstado) {
        LOGGER.debug("findByIdEstado idEstado {}", idEstado);

        TipoEmissao tipoEmissao = tipoEmissaoRepository.findByIdEstado(idEstado);
        if (tipoEmissao == null) {
            return this.findById(parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TP_EMISSAO).longValue()); //default pela pain
        }
        return tipoEmissao;
    }

    @GetMapping(value = "/tipoEmissao/{tpEmissao}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public TipoEmissao findById(@PathVariable(value = "tpEmissao", required = true) Long tpEmissao) {
        LOGGER.debug("findById tpEmissao {}", tpEmissao);

        TipoEmissao tipoEmissao = tipoEmissaoRepository.findById(tpEmissao);
        if (tipoEmissao == null) {
            throw new NotFoundException("Tipo de emiss\u00E3o " + tpEmissao + " n\u00E3o encontrado.");
        }
        return tipoEmissao;
    }

}
