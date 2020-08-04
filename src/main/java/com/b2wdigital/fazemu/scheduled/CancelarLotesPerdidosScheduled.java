package com.b2wdigital.fazemu.scheduled;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.LoteService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.Lote;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;
import com.b2wdigital.fazemu.exception.FazemuScheduledException;
import com.b2wdigital.fazemu.integration.dao.redis.RedisOperationsDao;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.LoteUtils;
import com.google.common.collect.Lists;
import java.net.UnknownHostException;

/**
 * Cancelar Lotes Perdidos.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class CancelarLotesPerdidosScheduled {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelarLotesPerdidosScheduled.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    private static final String KEY = RedisOperationsDao.KEY_SEMAFORO_CANCELAR_LOTES_PERDIDOS;
    private static final String LOCK_HOST_EM_PROCESSAMENTO = "LOCK_CANCELAR_LOTES_PERDIDOS";
    private static final String LOCK_EM_SELECAO_DE_DOCUMENTOS = "LOCK_CANCELAR_LOTES_PERDIDOS_EM_SELECAO_DE_DOCUMENTOS";

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Autowired
    private LoteService loteService;

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Scheduled(fixedDelay = 180_000L) //de 3 em 3 minutos
    public void cancelarLotesPerdidos() throws UnknownHostException {

        String statusScheduled = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_SCHEDULED_CANCELAR_LOTES_PERDIDOS);
        if (!"ON".equals(statusScheduled)) {
            throw new FazemuScheduledException("Chave de robo desligada");
        }

        String myHostLock = LoteUtils.getMyHost() + "_" + LOCK_HOST_EM_PROCESSAMENTO;

        if (cacheLoteRepository.setIfAbsent(myHostLock, true, 3, TimeUnit.MINUTES)) {

            List<Lote> lotesEleitos = getLotesElegiveis();
            if (CollectionUtils.isNotEmpty(lotesEleitos)) {
                String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

                lotesEleitos
                        .stream()
                        .forEach(lote -> {

                            try {
                                LOGGER.info("cancelarLotesPerdidos (scheduled) - cancelando lote {} automaticamente", lote.getId());
                                loteService.cancelarLote(lote.getId(), usuario);

                                //Remove do Redis
                                if (SituacaoLoteEnum.ABERTO.getCodigo().equals(lote.getSituacao())) {
                                    cacheLoteRepository.removerLoteAberto(lote.getId());
                                } else if (SituacaoLoteEnum.FECHADO.getCodigo().equals(lote.getSituacao())) {
                                    cacheLoteRepository.removerLoteFechado(lote.getId());
                                } else if (SituacaoLoteEnum.ENVIADO.getCodigo().equals(lote.getSituacao())) {
                                    cacheLoteRepository.removerLoteEnviado(lote.getId());
                                }
                            } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                            }

                        });
            }

            // Excluir os eleitos do semaforo
            removerMembrosSet(lotesEleitos);

            redisOperationsService.expiresKey(myHostLock, 1L, TimeUnit.MILLISECONDS);
        }

        LOGGER.info("cancelarLotesPerdidos (scheduled) - FIM");
    }

    private List<Lote> getLotesElegiveis() {

        List<Lote> listaLotesEleitos = Lists.newArrayList();

        if (cacheLoteRepository.setIfAbsent(LOCK_EM_SELECAO_DE_DOCUMENTOS, true, 1, TimeUnit.MINUTES)) {

            try {

                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MINUTE, -parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_MIN_EMITIR_DOCUMENTOS_PARADOS, 10));

                Calendar cal2 = Calendar.getInstance();
                cal2.add(Calendar.MINUTE, -parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_MAX_EMITIR_DOCUMENTOS_PARADOS, 300));

                // Limita o processo a quantidade pre estabelecida
                int count = 0;
                Integer docsPorProcesso = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_DOCUMENTOS_PROCESSADOS_EM_PARALELO, 30);

                //Verifica se o lote esta aberto/enviado
                List<Lote> listaLotesAbertos = loteService.listByDateIntervalAndSituacao(cal.getTime(), cal2.getTime(), SituacaoLoteEnum.ABERTO);
                List<Lote> listaLotesFechados = loteService.listByDateIntervalAndSituacao(cal.getTime(), cal2.getTime(), SituacaoLoteEnum.FECHADO);
                List<Lote> listaLotesEnviados = loteService.listByDateIntervalAndSituacao(cal.getTime(), cal2.getTime(), SituacaoLoteEnum.ENVIADO);

                if (CollectionUtils.isNotEmpty(listaLotesAbertos) || CollectionUtils.isNotEmpty(listaLotesFechados) || CollectionUtils.isNotEmpty(listaLotesEnviados)) {
                    List<Lote> listaLotes = Lists.newArrayList();
                    listaLotes.addAll(listaLotesAbertos);
                    listaLotes.addAll(listaLotesFechados);
                    listaLotes.addAll(listaLotesEnviados);

                    for (Lote lote : listaLotes) {
                        listaLotesEleitos.add(lote);
                        redisOperationsService.addToSet(KEY, lote.getId());
                        count++;
                        if (count == docsPorProcesso) {
                            break;
                        }
                    }

                    if (CollectionUtils.isNotEmpty(listaLotesEleitos)) {
                        redisOperationsService.expiresKey(KEY, 5L, TimeUnit.MINUTES);
                    }

                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        redisOperationsService.expiresKey(LOCK_EM_SELECAO_DE_DOCUMENTOS, 1L, TimeUnit.MILLISECONDS);
        return listaLotesEleitos;
    }

    protected void removerMembrosSet(List<Lote> lista) {
        for (Lote lote : lista) {
            redisOperationsService.removeFromSet(KEY, lote.getId());
        }
    }

}
