package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.service.ResponsavelTecnicoService;
import com.b2wdigital.fazemu.domain.ResponsavelTecnico;
import com.b2wdigital.fazemu.domain.form.ResponsavelTecnicoForm;
@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ResponsavelTecnicoController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResponsavelTecnicoController.class);
	private static final String DEFAULT_MAPPING = "/responsavelTecnico";

	@Autowired
	private ResponsavelTecnicoService responsavelTecnicoService;
	
    private Locale locale = LocaleContextHolder.getLocale();
    @Autowired
    private MessageSource ms;

	
	@GetMapping(value = DEFAULT_MAPPING)
	public List<ResponsavelTecnico> listByFiltros(@RequestParam Map<String, String> parameters) {

		return responsavelTecnicoService.listByFiltros(parameters);
	}

	
	@PostMapping(value = DEFAULT_MAPPING + "/adicionar")
	public ResponseEntity<Object> insert(@Valid @RequestBody ResponsavelTecnicoForm form) {

		LOGGER.info("insert form {}", form);

		responsavelTecnicoService.insert(ResponsavelTecnico.build(form));
		return new ResponseEntity<Object>(ms.getMessage("responsavelTecnico.success.added", null, locale), HttpStatus.CREATED);

	}

	
	@PostMapping(value = DEFAULT_MAPPING + "/atualizar")
	public ResponseEntity<Object>  update(@Valid @RequestBody ResponsavelTecnicoForm form) throws Exception {
			LOGGER.info("update form {}", form);

			ResponsavelTecnico responsavelTecnico = ResponsavelTecnico.build(form);
			responsavelTecnico.setIdResponsavelTecnico(form.getIdResponsavelTecnico());

			responsavelTecnicoService.update(responsavelTecnico);
			//form.setMensagemRetorno("Responsavel Técnico atualizado com sucesso");
			return new ResponseEntity<Object>(ms.getMessage("responsavelTecnico.success.updated",null, locale),HttpStatus.OK);
		
	}

}
