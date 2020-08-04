package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.service.EmissorRaizFilialService;
import com.b2wdigital.fazemu.business.service.EmissorRaizService;
import com.b2wdigital.fazemu.domain.EmissorRaiz;
import com.b2wdigital.fazemu.domain.EmissorRaizFilial;
import com.b2wdigital.fazemu.domain.form.EmissorRaizFilialForm;

@CrossOrigin(origins = { "http://localhost:3000" })
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class EmissorRaizFilialController {

	private static final String DEFAULT_MAPPING = "/emissorRaizFilial";
	private static final Logger LOGGER = LoggerFactory.getLogger(EmissorRaizFilialController.class);
	
	private Locale locale = LocaleContextHolder.getLocale();
    
    @Autowired
    private MessageSource ms;

	private ModelMapper modelMapper = new ModelMapper();

	@Autowired
	private EmissorRaizFilialService emissorRaizFilialService;

	@Autowired
	private EmissorRaizService emissorRaizService;

	@GetMapping(value = DEFAULT_MAPPING)
	public List<EmissorRaizFilialForm> listByFiltros(@RequestParam Map<String, String> parameters) {
		List<EmissorRaizFilialForm> resultado = new ArrayList<EmissorRaizFilialForm>();
		try {
			List<EmissorRaizFilial> listaERF;
			listaERF = emissorRaizFilialService.listByFiltros(parameters);
			for (EmissorRaizFilial erf : listaERF) {
				resultado.add(convertToForm(erf));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOGGER.error(ExceptionUtils.getStackTrace(e),e);
		}
		
		return resultado;
	}
	
	@PostMapping(value = DEFAULT_MAPPING + "/adicionar", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public EmissorRaizFilialForm insert(@Valid @RequestBody EmissorRaizFilialForm form)  {
		LOGGER.info("insert form {}", form);

		try {
			EmissorRaizFilial emissorFilial = new ModelMapper().map(form, EmissorRaizFilial.class);
			emissorRaizFilialService.insert(emissorFilial);
			form.setRetorno(ms.getMessage("emissorFilial.success.added", null, locale ));
			form.setSuccess(true);
			
		}catch (Exception e) {
			form.setRetorno(ms.getMessage("emissorFilial.error.added", null, locale ) + " - " + ExceptionUtils.getRootCauseMessage(e));
			form.setSuccess(false);
			LOGGER.error(ExceptionUtils.getStackTrace(e),e);
		}
		return form;
	}
	
	@PostMapping(value = DEFAULT_MAPPING + "/atualizar", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public EmissorRaizFilialForm update(@Valid @RequestBody EmissorRaizFilialForm form) {
		LOGGER.info("update form {}", form);
		
		try {
			EmissorRaizFilial emissorFilial = new ModelMapper().map(form, EmissorRaizFilial.class);
			emissorRaizFilialService.update(emissorFilial);
			form.setRetorno(ms.getMessage("emissorFilial.success.updated", null, locale ));
			form.setSuccess(true);
		
		}catch (Exception e) {
			form.setRetorno(ms.getMessage("emissorFilial.error.updated", null, locale ) + " - " + ExceptionUtils.getRootCauseMessage(e));
			form.setSuccess(false);
			LOGGER.error(ExceptionUtils.getStackTrace(e),e);
		}
		
		return form;
	}

	private EmissorRaizFilialForm convertToForm(EmissorRaizFilial erf) {
		EmissorRaizFilialForm form = modelMapper.map(erf, EmissorRaizFilialForm.class);
		form.setNomeEmissorRaiz(achaNomeDoEmissorRaiz(form));
		return form;
	}

	private String achaNomeDoEmissorRaiz(EmissorRaizFilialForm form) {
		Map<String, String> filtro = new HashMap<String, String>();
		filtro.put("id", String.valueOf(form.getIdEmissorRaiz()));
		EmissorRaiz er = emissorRaizService.listByFiltros(filtro).get(0);
		return er.getNome();
	}
	
	
}
