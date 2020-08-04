package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.service.DocumentoEventoService;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import org.springframework.web.bind.annotation.RequestParam;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class DocumentoEventoController {

    private static final String DEFAULT_MAPPING = "/documentoEvento";

    @Autowired
    private DocumentoEventoService documentoEventoService;

    @GetMapping(value = DEFAULT_MAPPING)
    public List<DocumentoEvento> findByIdDocFiscal(@RequestParam(value = "idDocFiscal", required = false) Long idDocFiscal) {
    	return  documentoEventoService.listByIdDocFiscal(idDocFiscal);
    }

}
