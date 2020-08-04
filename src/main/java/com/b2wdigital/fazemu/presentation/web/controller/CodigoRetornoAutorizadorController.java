package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.service.CodigoRetornoAutorizadorService;
import com.b2wdigital.fazemu.domain.form.CodigoRetornoAutorizadorForm;
import com.b2winc.corpserv.message.exception.NotFoundException;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web",produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class CodigoRetornoAutorizadorController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CodigoRetornoAutorizadorController.class);
    private static final String DEFAULT_MAPPING = "/codigoRetornoAutorizador";
    
    @Autowired
    private CodigoRetornoAutorizadorService codigoRetornoAutorizadorService;

    @GetMapping(value = DEFAULT_MAPPING)
    public List<CodigoRetornoAutorizadorForm> listAll() {
        return codigoRetornoAutorizadorService.listAll();
    }

	@GetMapping(value = DEFAULT_MAPPING + "/{id}")
	public List<CodigoRetornoAutorizadorForm> findById(@PathVariable(value = "id", required = true) Integer cStat) throws NotFoundException {
		try {
			LOGGER.debug("CodigoRetornoAutorizadorController: findById");
			return codigoRetornoAutorizadorService.listByCodigo(cStat);
		} catch (Exception e) {
			throw new NotFoundException("Codigo de retorno autorizador n\u00E3o encontrado para o codigo " + cStat);
		}
	}
    
}
