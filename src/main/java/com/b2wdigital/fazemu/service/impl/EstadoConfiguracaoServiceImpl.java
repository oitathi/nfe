package com.b2wdigital.fazemu.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.EstadoConfiguracaoRepository;
import com.b2wdigital.fazemu.business.service.EstadoConfiguracaoService;
import com.b2wdigital.fazemu.domain.EstadoConfiguracao;
import com.google.common.collect.Lists;

/**
 * Estado Configuracao Configuracao Service.
 *
 * @author Marcelo Oliveira {marcelo.doliveira@b2wdigital.com}
 * @version 1.0
 */
@Service
public class EstadoConfiguracaoServiceImpl implements EstadoConfiguracaoService {

    @Autowired
    private EstadoConfiguracaoRepository estadoConfiguracaoRepository;

    @Override
    public List<EstadoConfiguracao> listAll() {
        return estadoConfiguracaoRepository.listAll();
    }

    @Override
    public List<EstadoConfiguracao> listByFiltros(String tipoDocumentoFiscal, Long idEstado) {
        List<EstadoConfiguracao> lista = Lists.newArrayList();

        if (idEstado != null) {
            EstadoConfiguracao ec = estadoConfiguracaoRepository.findByTipoDocumentoFiscalAndIdEstado(tipoDocumentoFiscal, idEstado);
            lista.add(ec);
            return lista;
        } else {
            return estadoConfiguracaoRepository.listAll();
        }

    }

    @Override
    public void update(EstadoConfiguracao estadoConfiguracao) {
        estadoConfiguracaoRepository.update(estadoConfiguracao);
    }

}
