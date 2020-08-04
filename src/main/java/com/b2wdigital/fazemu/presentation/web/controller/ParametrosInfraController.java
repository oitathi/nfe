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

import com.b2wdigital.fazemu.business.service.ParametrosInfraService;
import com.b2wdigital.fazemu.domain.ParametrosInfra;
import com.b2wdigital.fazemu.domain.form.ParametrosInfraForm;
import com.b2wdigital.fazemu.exception.NotFoundException;
@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ParametrosInfraController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametrosInfraController.class);
    private static final String DEFAULT_MAPPING = "/parametrosInfra";

    @Autowired
    private ParametrosInfraService parametrosInfraService;

    @GetMapping(value = DEFAULT_MAPPING)
    public List<ParametrosInfra> listAll() {
        return parametrosInfraService.listAll();
    }

    @GetMapping(value = DEFAULT_MAPPING + "/findByTipoDocumentoFiscalAndIdParametro")
    public ParametrosInfra findByTipoDocumentoFiscalAndIdParametro(@RequestParam(value = "tipoDocumentoFiscal", required = true) String tipoDocumentoFiscal,
            @RequestParam(value = "idParametro", required = true) String idParametro) {
        ParametrosInfra valor = parametrosInfraService.findByTipoDocumentoFiscalAndIdParametro(tipoDocumentoFiscal, idParametro);
        if (valor == null) {
            throw new NotFoundException("Par\u00E2metro '" + idParametro + "' n\u00E3o encontrado.");
        }
        return valor;
    }

    @PostMapping(value = DEFAULT_MAPPING + "/atualizar")
    public ParametrosInfraForm update(@RequestBody ParametrosInfraForm form) throws Exception {
        try {
            LOGGER.info("update form {}", form);

            ParametrosInfra parametro = ParametrosInfra.build(form.getIdParametro(), form.getTipoDocumentoFiscal(), form.getValor(), form.getDescricao(), form.getTipo(), form.getUsuario());

            parametrosInfraService.update(parametro);
            form.setMensagemRetorno("Parametro atualizado com sucesso");

            return form;
        } catch (Exception e) {
            throw new NotFoundException(e.getMessage());
        }
    }

}
