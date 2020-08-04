package com.b2wdigital.fazemu.scheduled.disabled;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.LoteOperationsService;
import com.b2wdigital.fazemu.business.service.LoteService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.business.service.TipoEmissaoService;
import com.b2wdigital.fazemu.domain.Lote;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;
import com.b2wdigital.fazemu.integration.dao.redis.RedisOperationsDao;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.google.common.collect.Lists;

/**
 * Reconstruir Lotes Enviados Parados.
 *
 * @author Marcelo Oliveira {marcelo.doliveira@b2wdigital.com}
 * @version 1.0
 */
@Service
public class ReconstruirLotesEnviadosParadosScheduled {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconstruirLotesEnviadosParadosScheduled.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    private static final String KEY = RedisOperationsDao.KEY_SEMAFORO_RECONSTRUIR_LOTES_ENVIADOS_PARADOS;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Autowired
    protected TipoEmissaoService tipoEmissaoService;

    @Autowired
    private LoteService loteService;

    @Autowired
    private LoteOperationsService loteOperationsService;

//    @Scheduled(fixedDelay = 300_000L) //de 5 em 5 minutos
    public void reconstruirLotesEnviadosParados() {
        List<Lote> lotesEnviadosParadosEleitos = getLotesElegiveis();

        if (CollectionUtils.isNotEmpty(lotesEnviadosParadosEleitos)) {

            lotesEnviadosParadosEleitos.stream()
                    .forEach(lote -> {
                        try {
                            LOGGER.debug("reconstruirLotesEnviadosParados (scheduled) - reconstruirLote {}", lote.getId());

                            loteOperationsService.reconstruirLote(lote.getId());
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    });
        }

        // Excluir os eleitos do semaforo
        removerMembrosLista(lotesEnviadosParadosEleitos);
    }

    private List<Lote> getLotesElegiveis() {
        // Quatro minutos atras
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_MIN_LOTES_ENVIADOS_PARADOS, 10));

        // Quarenta minutos atras
        Calendar cal2 = Calendar.getInstance();
        cal2.add(Calendar.MINUTE, -parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_MAX_LOTES_ENVIADOS_PARADOS, 120));

        // Limita o processo a quantidade pre estabelecida
        int count = 0;
        Integer docsPorProcesso = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_DOCUMENTOS_PROCESSADOS_EM_PARALELO, 30);
        List<Lote> lotesElegiveis = Lists.newArrayList();

        List<Lote> listaLote = loteService.listByDateIntervalAndSituacao(cal.getTime(), cal2.getTime(), SituacaoLoteEnum.ENVIADO);
        LOGGER.info("ReconstruirLotesEnviadosParadosScheduled: getLotesElegiveis {}", listaLote);

        if (CollectionUtils.isNotEmpty(listaLote)) {

            for (Lote lote : listaLote) {

                lotesElegiveis.add(lote);

                redisOperationsService.addToSet(KEY, lote.getId());
                count++;
                if (count == docsPorProcesso) {
                    break;
                }
            }

            if (CollectionUtils.isNotEmpty(lotesElegiveis)) {
                redisOperationsService.expiresKey(KEY, 5L, TimeUnit.MINUTES);
            }
        }

        return lotesElegiveis;
    }

    private void removerMembrosLista(List<Lote> lista) {
        for (Lote lote : lista) {
            redisOperationsService.removeFromSet(KEY, lote.getId());
        }
    }

}
