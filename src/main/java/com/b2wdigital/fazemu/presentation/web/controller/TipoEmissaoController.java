package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.service.TipoEmissaoService;
import com.b2wdigital.fazemu.domain.TipoEmissao;
import com.b2wdigital.fazemu.domain.form.TipoEmissaoForm;
@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web",produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class TipoEmissaoController {
    private static final String DEFAULT_MAPPING = "/tipoEmissao";
    
    @Autowired
    private TipoEmissaoService tipoEmissaoService;
    
    @GetMapping(value = DEFAULT_MAPPING)
    public List<TipoEmissaoForm> listAll() {
        return tipoEmissaoService.listAll();
    }
    
    @GetMapping(value = DEFAULT_MAPPING + "/ativos")
    public List<TipoEmissao> listAtivos() {
        return tipoEmissaoService.listAtivos();
    }
    
    @GetMapping(value = DEFAULT_MAPPING + "/estado")
    public List<TipoEmissao> listByIdEstado(@RequestParam Map<String, String> parameters) throws Exception {
        return tipoEmissaoService.listByIdEstado(parameters);
    }
    
}
