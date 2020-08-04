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

import com.b2wdigital.fazemu.business.service.EstadoTipoEmissaoService;
import com.b2wdigital.fazemu.domain.EstadoTipoEmissao;
import com.b2wdigital.fazemu.domain.form.EstadoTipoEmissaoForm;
import com.b2wdigital.fazemu.utils.DateUtils;
import com.b2winc.corpserv.message.exception.NotFoundException;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class EstadoTipoEmissaoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EstadoTipoEmissaoController.class);
    private static final String DEFAULT_MAPPING = "/estadoTipoEmissao";

    @Autowired
    private EstadoTipoEmissaoService estadoTipoEmissaoService;

    @GetMapping(value = DEFAULT_MAPPING)
    public List<EstadoTipoEmissao> listByFiltros(@RequestParam(value = "idEstado", required = false) Long idEstado,
            @RequestParam(value = "dataHoraInicio", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date dataHoraInicio,
            @RequestParam(value = "dataHoraFim", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date dataHoraFim,
            @RequestParam(value = "tipoEmissao", required = false) Integer tipoEmissao) {
        return estadoTipoEmissaoService.listByFiltros(idEstado, dataHoraInicio, dataHoraFim, tipoEmissao);
    }

    @PostMapping(value = DEFAULT_MAPPING + "/adicionar")
    public EstadoTipoEmissaoForm insert(@RequestBody EstadoTipoEmissaoForm form) throws Exception {
        try {
            LOGGER.info("insert form {}", form);

            estadoTipoEmissaoService.insert(EstadoTipoEmissao.build(Long.valueOf(form.getIdEstado()),
                    DateUtils.iso8601ToCalendar(form.getDataInicioEmissao()).getTime(),
                    DateUtils.iso8601ToCalendar(form.getDataFimEmissao()).getTime(),
                    Long.valueOf(form.getTipoEmissao()), form.getJustificativa(), form.getUsuario()));
            form.setMensagemRetorno("Estado Tipo Emissao incluido com sucesso");

            return form;
        } catch (Exception e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    @PostMapping(value = DEFAULT_MAPPING + "/atualizar")
    public EstadoTipoEmissaoForm update(@RequestBody EstadoTipoEmissaoForm form) throws Exception {
        try {
            LOGGER.info("update form {}", form);

            EstadoTipoEmissao estadoTipoEmissao = EstadoTipoEmissao.build(Long.valueOf(form.getIdEstado()),
                    DateUtils.iso8601ToCalendar(form.getDataInicioEmissao()).getTime(),
                    DateUtils.iso8601ToCalendar(form.getDataFimEmissao()).getTime(),
                    Long.valueOf(form.getTipoEmissao()), form.getJustificativa(), form.getUsuario());

            estadoTipoEmissaoService.update(estadoTipoEmissao);
            form.setMensagemRetorno("Estado Tipo Emissao atualizado com sucesso");

            return form;
        } catch (Exception e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    @GetMapping(value = DEFAULT_MAPPING + "/tipoEmissaoAtivo")
    public List<EstadoTipoEmissao> listByTipoEmissaoAtivo() {
        return estadoTipoEmissaoService.listByTipoEmissaoAtivo();
    }

}
