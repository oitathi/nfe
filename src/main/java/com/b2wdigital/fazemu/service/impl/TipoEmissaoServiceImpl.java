package com.b2wdigital.fazemu.service.impl;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.repository.TipoEmissaoRepository;
import com.b2wdigital.fazemu.business.service.TipoEmissaoService;
import com.b2wdigital.fazemu.domain.TipoEmissao;
import com.b2wdigital.fazemu.domain.form.TipoEmissaoForm;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.google.common.collect.Lists;

/**
 * Tipo Emissao Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class TipoEmissaoServiceImpl implements TipoEmissaoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TipoEmissaoServiceImpl.class);

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private TipoEmissaoRepository tipoEmissaoRepository;

    @Override
    public Integer getTipoEmissaoByCodigoIBGE(String codigoIBGE) {
        LOGGER.debug("TipoEmissaoServiceImpl: getTipoEmissaoByCodigoIBGE");
        Integer tipoEmissaoAtual = estadoRepository.getTipoEmissaoByCodigoIbge(Integer.valueOf(codigoIBGE));
        if (tipoEmissaoAtual == null) {
            tipoEmissaoAtual = parametrosInfraRepository.getAsInteger(FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase(), ParametrosInfraRepository.PAIN_TP_EMISSAO);
        }

        if (tipoEmissaoAtual == null) {
            throw new FazemuServiceException("Tipo de Emissao nao cadastrado para UF: " + codigoIBGE);
        }

        return tipoEmissaoAtual;
    }

    @Override
    public List<TipoEmissaoForm> listAll() {

        List<TipoEmissao> listaTipoEmissao = tipoEmissaoRepository.listAll();
        if (listaTipoEmissao.isEmpty() || listaTipoEmissao == null) {
            throw new FazemuServiceException("Tipo de Emissao nao encontrado");
        }

        return toFormList(listaTipoEmissao);
    }

    @Override
    public List<TipoEmissao> listAtivos() {
        return tipoEmissaoRepository.listAtivos();
    }

    private List<TipoEmissaoForm> toFormList(List<TipoEmissao> listaTipoEmissao) {
        List<TipoEmissaoForm> listaTipoEmissaoForm = Lists.newArrayList();

        for (TipoEmissao tipoEmissao : listaTipoEmissao) {
            TipoEmissaoForm form = new TipoEmissaoForm();
            form.setId(tipoEmissao.getId().toString());
            form.setNome(tipoEmissao.getNome());
            form.setSituacao(tipoEmissao.getSituacao());
            listaTipoEmissaoForm.add(form);
        }

        return listaTipoEmissaoForm;
    }

    @Override
    public TipoEmissao findByIdTipoEmissao(Long idTipoEmissao) {
        return tipoEmissaoRepository.findById(idTipoEmissao);
    }

    @Override
    public List<TipoEmissao> listByIdEstado(Map<String, String> parameters) throws Exception {
        return tipoEmissaoRepository.listByIdEstado(parameters);
    }

}
