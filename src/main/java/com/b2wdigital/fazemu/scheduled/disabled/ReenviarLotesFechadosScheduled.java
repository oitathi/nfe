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
 * Reenviar Lotes Fechados.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class ReenviarLotesFechadosScheduled {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReenviarLotesFechadosScheduled.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    private static final String KEY = RedisOperationsDao.KEY_SEMAFORO_REENVIAR_LOTES_FECHADOS;

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private FecharEnviarLoteService fecharEnviarLoteService;

    @Autowired
    private RedisOperationsService redisOperationsService;

//    @Scheduled(fixedDelay = 300_000L) //de 5 em 5 minutos
    public void reenviarLotesFechados() {
        Set<Object> lotesFechadosEleitos = getLotesElegiveis();

        try {
            if (CollectionUtils.isNotEmpty(lotesFechadosEleitos)) {

                long tempoEspera = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_REENVIO_LOTES_FECHADOS, 300); //default 300 seg
                lotesFechadosEleitos
                        .stream()
                        .map(obj -> ((Integer) obj).longValue()) //obj == idLote em tipo Integer
                        .map(idLote -> cacheLoteRepository.consultarLote(idLote))
                        .filter(lote -> lote != null && FazemuUtils.deltaInSeconds(new Date(), lote.getDataAbertura()) > tempoEspera)
                        .forEach(lote -> {
                            LOGGER.info("reenviarLotesFechados (scheduled) - lotesFechadoEleito {}", lote.getIdLote());
                            fecharEnviarLoteService.fecharEnviarLote(lote.getIdLote());
                        });
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        // Excluir os eleitos do semaforo
        removerMembrosSet(lotesFechadosEleitos);
    }

    protected Set<Object> getLotesElegiveis() {

        Set<Object> lotesElegiveis = Sets.newHashSet();
        Set<Object> lotesAProcessar = redisOperationsService.difference("lotesFechados", KEY);

        if (CollectionUtils.isNotEmpty(lotesAProcessar)) {

            // Ordena set
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
