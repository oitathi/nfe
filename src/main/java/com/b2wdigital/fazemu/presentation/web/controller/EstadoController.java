package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.service.EstadoService;
import com.b2wdigital.fazemu.domain.Estado;
@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class EstadoController {

    private static final String DEFAULT_MAPPING = "/estado";

    @Autowired
    private EstadoService estadoService;

    @GetMapping(value = DEFAULT_MAPPING)
    public List<Estado> listAll() {
        return estadoService.listAll();
    }

    @GetMapping(value = DEFAULT_MAPPING + "/ativo")
    public List<Estado> listByAtivo() {
        return estadoService.listByAtivo();
    }

}
