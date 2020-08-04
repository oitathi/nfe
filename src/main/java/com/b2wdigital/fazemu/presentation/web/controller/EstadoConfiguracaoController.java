package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.service.EstadoConfiguracaoService;
import com.b2wdigital.fazemu.domain.EstadoConfiguracao;
import com.b2wdigital.fazemu.domain.form.EstadoConfiguracaoForm;
import com.b2wdigital.fazemu.exception.NotFoundException;
@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class EstadoConfiguracaoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EstadoConfiguracaoController.class);
    private static final String DEFAULT_MAPPING = "/estadoConfiguracao";

    @Autowired
    private EstadoConfiguracaoService estadoConfiguracaoService;

    @GetMapping(value = DEFAULT_MAPPING)
    public List<EstadoConfiguracao> listByFiltros(@RequestParam(value = "tipoDocumentoFiscal", required = false) String tipoDocumentoFiscal,
            @RequestParam(value = "idEstado", required = false) Long idEstado) {
        return estadoConfiguracaoService.listByFiltros(tipoDocumentoFiscal, idEstado);
    }

    @PostMapping(value = DEFAULT_MAPPING + "/atualizar")
    public EstadoConfiguracaoForm update(@RequestBody EstadoConfiguracaoForm form) throws Exception {
        try {
            LOGGER.info("update form {}", form);

            EstadoConfiguracao estadoConfiguracao = EstadoConfiguracao.build(form.getTipoDocumentoFiscal(), form.getInAtivo(), form.getInResponsavelTecnico(), form.getInCSRT(), form.getInEPECAutomatico(), form.getQuantidadeMinimaRegistros(), form.getPeriodo(), form.getPeriodoEPEC(), form.getUsuario());
            estadoConfiguracao.setIdEstado(form.getIdEstado());

            estadoConfiguracaoService.update(estadoConfiguracao);
            form.setMensagemRetorno("Estado Configuracao atualizado com sucesso");

            return form;
        } catch (Exception e) {
            throw new NotFoundException(e.getMessage());
        }
    }

}
