package com.b2wdigital.fazemu.scheduled.disabled;

import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.repository.RedisOperationsRepository;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.LoteUtils;

/**
 * Obtem lotes de hosts perdidos, e renomeia para jogar novamente na esteira do
 * processo.
 *
 * @author Rodrigo Matos Silva {rodrigo.matos@b2wdigital.com}
 * @version 1.0
 */
@Service
public class LotesPerdidosScheduled {

    private static final Logger LOGGER = LoggerFactory.getLogger(LotesPerdidosScheduled.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    private RedisOperationsRepository redisOperationsRepository;

    @Autowired
    private RedisOperations<String, Object> redisOperations;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    private String LOTE_PERDIDOS_EM_PROC = "LOTE_PERDIDOS_EM_PROC";

    private long tempoEspera;

    @PostConstruct
    public void init() {
        tempoEspera = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_ESPERA_FECHAMENTO_LOTE, 15);
    }

//	@Scheduled(fixedDelay = 900_000L) // de 15 em 15 minutos
    public void changeIdLotesPerdidos() throws NumberFormatException, UnknownHostException {

        if (cacheLoteRepository.setIfAbsent(LOTE_PERDIDOS_EM_PROC, Calendar.getInstance().getTimeInMillis(),
                tempoEspera * 2, TimeUnit.MILLISECONDS)) {

            Set<String> keys = redisOperationsRepository.allKeysByPattern("*_".concat(LoteUtils.LOTE_LITERAL).concat("_*"));

            if (CollectionUtils.isNotEmpty(keys)) {
                LOGGER.info("changeIdLotesPerdidos (scheduled) - keys {}", keys);

                // Limita o processo a quantidade pre estabelecida
//				Integer docsPorProcesso = parametrosInfraRepository.getAsInteger(ParametrosInfraRepository.DOCUMENTOS_PROCESSADOS_EM_PARALELO, 30);
                Integer docsPorProcesso = 15;
                Set<String> myHostkeys = redisOperationsRepository.allKeysByPattern(LoteUtils.getMyHost().concat("_").concat(LoteUtils.LOTE_LITERAL).concat("_*"));
                int count = docsPorProcesso - myHostkeys.size();

                for (String oldKey : keys) {

                    ResumoLote value = (ResumoLote) redisOperationsRepository.getKeyValue(oldKey);

                    if (value.getDataUltimaAlteracao() != null && FazemuUtils.deltaInSeconds(new Date(), value.getDataUltimaAlteracao()) > 900) {

                        value.setDataUltimaAlteracao(new Date());

                        String newKey = LoteUtils.getLoteIdRedis(Long.parseLong(oldKey.split("_")[2]));
                        redisOperations.boundValueOps(newKey).set(value);
                        cacheLoteRepository.deleteKey(oldKey);
                    }

                    count++;
                    if (count == docsPorProcesso) {
                        break;
                    }
                }
            }
            cacheLoteRepository.deleteKey(LOTE_PERDIDOS_EM_PROC);
        }
    }
}
