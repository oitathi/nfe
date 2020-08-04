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
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.service.ImpressoraService;
import com.b2wdigital.fazemu.domain.Impressora;
import com.b2wdigital.fazemu.domain.form.ImpressoraForm;
import com.b2winc.corpserv.message.exception.NotFoundException;
import org.springframework.web.bind.annotation.RequestParam;
@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ImpressoraController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImpressoraController.class);
    private static final String DEFAULT_MAPPING = "/impressora";

    @Autowired
    private ImpressoraService impressoraService;

    @GetMapping(value = DEFAULT_MAPPING)
    public List<Impressora> listByFiltros(@RequestParam(value = "nome", required = false) String nome,
            @RequestParam(value = "local", required = false) String local,
            @RequestParam(value = "ip", required = false) String ip,
            @RequestParam(value = "marca", required = false) String marca,
            @RequestParam(value = "modelo", required = false) String modelo,
            @RequestParam(value = "situacao", required = false) String situacao) {
        return impressoraService.listByFiltros(nome, local, ip, marca, modelo, situacao);
    }

    @PostMapping(value = DEFAULT_MAPPING + "/adicionar")
    public ImpressoraForm insert(@RequestBody ImpressoraForm form) throws Exception {
        try {
            LOGGER.info("insert form {}", form);

            impressoraService.insert(Impressora.build(form.getNome(), form.getLocal(), form.getIp(), form.getPorta(), form.getMarca(), form.getModelo(), form.getSituacao(), form.getUsuario()));
            form.setMensagemRetorno("Impressora incluida com sucesso");

            return form;
        } catch (Exception e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    @PostMapping(value = DEFAULT_MAPPING + "/atualizar")
    public ImpressoraForm update(@RequestBody ImpressoraForm form) throws Exception {
        try {
            LOGGER.info("update form {}", form);

            Impressora impressora = Impressora.build(form.getNome(), form.getLocal(), form.getIp(), form.getPorta(), form.getMarca(), form.getModelo(), form.getSituacao(), form.getUsuario());
            impressora.setId(Long.valueOf(form.getId()));

            impressoraService.update(impressora);
            form.setMensagemRetorno("Impressora atualizada com sucesso");

            return form;
        } catch (Exception e) {
            throw new NotFoundException(e.getMessage());
        }
    }

}
