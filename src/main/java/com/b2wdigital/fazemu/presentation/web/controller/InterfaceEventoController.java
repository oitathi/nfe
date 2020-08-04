package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.List;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.service.InterfaceEventoService;
import com.b2wdigital.fazemu.domain.InterfaceEvento;
import org.springframework.web.bind.annotation.RequestMapping;
@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class InterfaceEventoController {
    private static final String DEFAULT_MAPPING = "/interfaceEvento";
    
    @Autowired
    private InterfaceEventoService interfaceEventoService;
    
    @GetMapping(value = DEFAULT_MAPPING)
    public List<InterfaceEvento> listByFiltros(@RequestParam(value = "idSistema", required = false) String idSistema,
                                               @RequestParam(value = "idMetodo", required = false) Long idMetodo,
                                               @RequestParam(value = "chaveAcesso", required = false) String chaveAcesso,
                                               @RequestParam(value = "dataHoraRegistroInicio", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date dataHoraRegistroInicio,
                                               @RequestParam(value = "dataHoraRegistroFim", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date dataHoraRegistroFim,
                                               @RequestParam(value = "situacao", required = false) String situacao,
                                               @RequestParam(value = "quantidadeRegistros", required = false) Long quantidadeRegistros) {
            return interfaceEventoService.listByFiltros(idSistema, idMetodo, chaveAcesso, dataHoraRegistroInicio, dataHoraRegistroFim, situacao, quantidadeRegistros);
    }    
}
