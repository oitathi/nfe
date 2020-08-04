package com.b2wdigital.fazemu.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.CodigoRetornoAutorizadorRepository;
import com.b2wdigital.fazemu.business.service.CodigoRetornoAutorizadorService;
import com.b2wdigital.fazemu.domain.CodigoRetornoAutorizador;
import com.b2wdigital.fazemu.domain.form.CodigoRetornoAutorizadorForm;
import com.b2wdigital.fazemu.enumeration.CodigoRetornoAutorizadorEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.google.common.collect.Lists;

/**
 * CodigoRetornoAutorizador Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class CodigoRetornoAutorizadorServiceImpl implements CodigoRetornoAutorizadorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodigoRetornoAutorizadorServiceImpl.class);

    @Autowired
    private CodigoRetornoAutorizadorRepository codigoRetornoAutorizadorRepository;

    @Override
    public List<CodigoRetornoAutorizadorForm> listAll() {
        return toFormList(codigoRetornoAutorizadorRepository.listAll());
    }

    @Override
    public List<CodigoRetornoAutorizadorForm> listByCodigo(Integer cStat) {
        if (cStat == null) {
            throw new FazemuServiceException("Codigo Retorno Autorizador n\u00E3o informado");
        }

        CodigoRetornoAutorizador codigoRetornoAutorizador = codigoRetornoAutorizadorRepository.findById(cStat);
        if (codigoRetornoAutorizador == null) {
            throw new FazemuServiceException("Codigo Retorno Autorizador nao encontrado com o codigo " + cStat);
        }

        return objectToFormList(codigoRetornoAutorizador);
    }

    @Override
    public void insert(CodigoRetornoAutorizador codigoRetornoAutorizador) {
        codigoRetornoAutorizadorRepository.insert(codigoRetornoAutorizador);
    }

    @Override
    public void update(CodigoRetornoAutorizador codigoRetornoAutorizador) {
        codigoRetornoAutorizadorRepository.update(codigoRetornoAutorizador);
    }

    private List<CodigoRetornoAutorizadorForm> objectToFormList(CodigoRetornoAutorizador codigo) {

        List<CodigoRetornoAutorizador> listaCodigoRetornoAutorizador = Lists.newArrayList();
        listaCodigoRetornoAutorizador.add(codigo);

        return toFormList(listaCodigoRetornoAutorizador);
    }

    private List<CodigoRetornoAutorizadorForm> toFormList(List<CodigoRetornoAutorizador> listaCodigoRetorno) {
        List<CodigoRetornoAutorizadorForm> listaCodigoRetornoAutorizadorForm = Lists.newArrayList();

        listaCodigoRetorno.stream().map((codigo) -> {
            CodigoRetornoAutorizadorForm form = new CodigoRetornoAutorizadorForm();
            form.setId(codigo.getId().toString());
            form.setTipoDocumentoFiscal(codigo.getTipoDocumentoFiscal());
            form.setDescricao(codigo.getDescricao());
            form.setSituacaoAutorizador(codigo.getSituacaoAutorizador());
            form.setSituacaoAutorizadorTXT(CodigoRetornoAutorizadorEnum.getByCodigo(codigo.getSituacaoAutorizador()).getDescricao());
            form.setUsuario(codigo.getUsuario());
            form.setDataHora(codigo.getDataHora().toString());
            return form;
        }).forEachOrdered((form) -> {
            listaCodigoRetornoAutorizadorForm.add(form);
        });

        return listaCodigoRetornoAutorizadorForm;
    }

    @Override
    public String getSituacaoAutorizacaoById(Integer cStat) {
        LOGGER.debug("CodigoRetornoAutorizadorServiceImpl: getSituacaoAutorizacaoById CStat {}", cStat);
        if (cStat == null) {
            return null;
        }

        return codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(cStat);

    }

}
