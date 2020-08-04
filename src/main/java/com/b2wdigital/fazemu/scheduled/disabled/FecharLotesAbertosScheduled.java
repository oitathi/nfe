package com.b2wdigital.fazemu.scheduled.disabled;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.FecharEnviarLoteService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.integration.dao.redis.RedisOperationsDao;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.google.common.collect.Sets;

/**
 * Fechar Lotes Abertos.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class FecharLotesAbertosScheduled {

    private static final Logger LOGGER = LoggerFactory.getLogger(FecharLotesAbertosScheduled.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    private static final String KEY = RedisOperationsDao.KEY_SEMAFORO_FECHAR_LOTES_ABERTOS;

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private FecharEnviarLoteService fecharEnviarLoteService;

    @Autowired
    private RedisOperationsService redisOperationsService;

//    @Scheduled(fixedDelay = 30_000L) //de 30 em 30 segundos
    public void fecharLotesAbertos() {

        Set<Object> lotesAbertosEleitos = getLotesElegiveis();

        try {
            if (CollectionUtils.isNotEmpty(lotesAbertosEleitos)) {

                long tempoEspera = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_ESPERA_FECHAMENTO_LOTE, 15); //default 15seg
                lotesAbertosEleitos
                        .stream()
                        .map(obj -> ((Integer) obj).longValue()) //obj == idLote em tipo Integer
                        .map(idLote -> cacheLoteRepository.consultarLote(idLote))
                        .filter(lote -> lote != null)
                        .filter(lote -> lote != null && FazemuUtils.deltaInSeconds(new Date(), lote.getDataAbertura()) > tempoEspera)
                        .forEach(lote -> {
                            LOGGER.info("fecharLotesAbertos (scheduled) - fechando lotesAbertoEleito {}", lote.getIdLote());
                            fecharEnviarLoteService.fecharEnviarLote(lote.getIdLote());
                        });
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        // Excluir os eleitos do semaforo
        removerMembrosSet(lotesAbertosEleitos);
    }

    protected Set<Object> getLotesElegiveis() {

        Set<Object> lotesElegiveis = Sets.newHashSet();
        Set<Object> lotesAProcessar = redisOperationsService.difference("lotesAbertos", KEY);

        if (CollectionUtils.isNotEmpty(lotesAProcessar)) {

            //Ordena set
            TreeSet<Object> myTreeSet = new TreeSet<Object>();
            myTreeSet.addAll(lotesAProcessar);

            // Limita o processo a quantidade pre estabelecida
            int count = 0;
            Integer docsPorProcesso = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_DOCUMENTOS_PROCESSADOS_EM_PARALELO, 30);
            for (Object idLote : lotesAProcessar) {
                lotesElegiveis.add(idLote);
                redisOperationsService.addToSet(KEY, new Long((Integer) idLote));
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

    protected void removerMembrosSet(Set<Object> lista) {
        for (Object id : lista) {
            redisOperationsService.removeFromSet(KEY, new Long((Integer) id));
        }
    }

}
