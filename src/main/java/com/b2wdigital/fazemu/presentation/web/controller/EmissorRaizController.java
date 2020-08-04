package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.validation.Valid;

import org.modelmapper.ModelMapper;
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

import com.b2wdigital.fazemu.business.service.EmissorRaizService;
import com.b2wdigital.fazemu.domain.EmissorRaiz;
import com.b2wdigital.fazemu.domain.form.EmissorRaizForm;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class EmissorRaizController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmissorRaizController.class);
    private static final String DEFAULT_MAPPING = "/emissorRaiz";

    private Locale locale = LocaleContextHolder.getLocale();

    @Autowired
    private MessageSource ms;

    @Autowired
    private EmissorRaizService emissorRaizService;

    @GetMapping(value = DEFAULT_MAPPING)
    public List<EmissorRaizForm> listByFiltros(@RequestParam Map<String, String> parameters) {
        List<EmissorRaiz> emissoresRaiz = emissorRaizService.listByFiltros(parameters);
        return emissorRaizService.toForm(emissoresRaiz);
    }

    @PostMapping(value = DEFAULT_MAPPING + "/adicionar")
    public ResponseEntity<Object> insert(@Valid @RequestBody EmissorRaizForm form) throws Exception {
        LOGGER.info("insert form {}", form);

        EmissorRaiz emissorRaiz = new ModelMapper().map(form, EmissorRaiz.class);
        emissorRaizService.insert(emissorRaiz);

        return new ResponseEntity<Object>(ms.getMessage("emissor.success.added", null, locale), HttpStatus.CREATED);
    }

    @PostMapping(value = DEFAULT_MAPPING + "/atualizar")
    public ResponseEntity<Object> update(@Valid @RequestBody EmissorRaizForm form) throws Exception {

        LOGGER.info("update form {}", form);

        EmissorRaiz emissorRaiz = new ModelMapper().map(form, EmissorRaiz.class);
        emissorRaiz.setId(Long.valueOf(form.getId()));
        emissorRaizService.update(emissorRaiz);

        return new ResponseEntity<Object>(ms.getMessage("emissor.success.updated", null, locale), HttpStatus.OK);
    }

}
