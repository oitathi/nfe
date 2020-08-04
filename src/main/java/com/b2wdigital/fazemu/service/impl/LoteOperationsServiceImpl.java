package com.b2wdigital.fazemu.service.impl;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoLoteRepository;
import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.repository.LoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.LoteOperationsService;
import com.b2wdigital.fazemu.domain.Lote;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;

/**
 * LoteOperations Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class LoteOperationsServiceImpl implements LoteOperationsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoteOperationsServiceImpl.class);

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private DocumentoLoteRepository documentoLoteRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Override
    public void reconstruirLote(Long idLote) {
        LOGGER.info("reconstruirLote {} ", idLote);
        Lote lote = loteRepository.findById(idLote);
        if (lote != null) {
            long currentTimeMillis = System.currentTimeMillis();
            ResumoLote resumoLote = ResumoLote.fromLote(lote, documentoLoteRepository.listByIdDocFiscal(lote.getId()));
            resumoLote.setUf(estadoRepository.findById(lote.getIdEstado()).getCodigoIbge());
            resumoLote.setDataAbertura(new Date());

            if (SituacaoLoteEnum.ABERTO.getCodigo().equals(lote.getSituacao())) {
                LOGGER.info("Reconstruindo lote aberto {}", idLote);
                String key = "lotesAbertos" + currentTimeMillis;

                cacheLoteRepository.abrirLote(resumoLote, key);
                cacheLoteRepository.sobreporLotesAbertos(key);
            } else if (SituacaoLoteEnum.FECHADO.getCodigo().equals(lote.getSituacao())) {
                LOGGER.info("Reconstruindo lote fechado {}", idLote);
                String key = "lotesFechados" + currentTimeMillis;

                cacheLoteRepository.abrirLote(resumoLote, key); //eh a mesma funcao de abertura mesmo, porque nao foi enviado
                loteRepository.reabrirLote(idLote, parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT));
                cacheLoteRepository.sobreporLotesAbertos(key);
            } else if (SituacaoLoteEnum.ENVIADO.getCodigo().equals(lote.getSituacao())) {
                LOGGER.info("Reconstruindo lote enviado {}", idLote);
                String key = "lotesEnviados" + currentTimeMillis;

                cacheLoteRepository.atualizarLote(resumoLote);
                cacheLoteRepository.adicionarLoteEnviado(lote.getId());
                cacheLoteRepository.sobreporLotesEnviados(key);
            }
        }
    }

}
