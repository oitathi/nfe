package com.b2wdigital.fazemu.service.impl;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.EstadoTipoEmissaoRepository;
import com.b2wdigital.fazemu.business.repository.RedisOperationsRepository;
import com.b2wdigital.fazemu.business.service.EstadoTipoEmissaoService;
import com.b2wdigital.fazemu.domain.EstadoTipoEmissao;
import com.b2wdigital.fazemu.exception.FazemuServiceException;

/**
 * Estado Tipo Emissao Service.
 *
 * @author Marcelo Oliveira {marcelo.doliveira@b2wdigital.com}
 * @version 1.0
 */
@Service
public class EstadoTipoEmissaoServiceImpl implements EstadoTipoEmissaoService {

    @Autowired
    private EstadoTipoEmissaoRepository estadoTipoEmissaoRepository;

    @Autowired
    private RedisOperationsRepository redisOperationsRepository;

    @Override
    public void insert(EstadoTipoEmissao estadoTipoEmissao) {
        if (estadoTipoEmissao.getDataFim().compareTo(estadoTipoEmissao.getDataInicio()) < 0) {
            throw new FazemuServiceException("Erro ao converter datas de inicio " + estadoTipoEmissao.getDataInicio() + " e fim " + estadoTipoEmissao.getDataFim());
        }

        estadoTipoEmissaoRepository.insert(estadoTipoEmissao);
        expireCache();
    }

    @Override
    public void update(EstadoTipoEmissao estadoTipoEmissao) {
        estadoTipoEmissaoRepository.update(estadoTipoEmissao);
        expireCache();
    }

    @Override
    public List<EstadoTipoEmissao> listByFiltros(Long idEstado, Date dataHoraInicio, Date dataHoraFim, Integer tipoEmissao) {
        return estadoTipoEmissaoRepository.listByFiltros(idEstado, dataHoraInicio, dataHoraFim, tipoEmissao);
    }

    @Override
    public List<EstadoTipoEmissao> listByEstadoAndTipoEmissao(Long idEstado, Integer tipoEmissao) {
        return estadoTipoEmissaoRepository.listByEstadoAndTipoEmissao(idEstado, tipoEmissao);
    }

    private void expireCache() {
        //Expira o cache manualmente
        redisOperationsRepository.expiresKey("EstadoTipoEmissaoJdbcDao::listAll", 1L, TimeUnit.MILLISECONDS);
    }

    @Override
    public List<EstadoTipoEmissao> listByTipoEmissaoAtivo() {
        return estadoTipoEmissaoRepository.listByTipoEmissaoAtivo();
    }

}
