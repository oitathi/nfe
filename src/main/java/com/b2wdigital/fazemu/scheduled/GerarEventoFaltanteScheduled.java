package com.b2wdigital.fazemu.scheduled;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.ConsultarProtocoloService;
import com.b2wdigital.fazemu.business.service.DocumentoFiscalService;
import com.b2wdigital.fazemu.business.service.LoteService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.Lote;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.FazemuScheduledException;
import com.b2wdigital.fazemu.integration.dao.redis.RedisOperationsDao;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.LoteUtils;
import com.google.common.collect.Lists;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gerar Evento Faltante Scheduled.
 *
 * @author Marcelo Oliveira {marcelo.doliveira@b2wdigital.com}
 * @version 1.0
 */
@Service
public class GerarEventoFaltanteScheduled extends AbstractScheduled {

    private static final Logger LOGGER = LoggerFactory.getLogger(GerarEventoFaltanteScheduled.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    private static final String KEY = RedisOperationsDao.KEY_SEMAFORO_GERAR_EVENTO_FALTANTE;
    private static final String LOCK_HOST_EM_PROCESSAMENTO = "LOCK_GERAR_EVENTO_FALTANTE";
    private static final String LOCK_EM_SELECAO_DE_DOCUMENTOS = "LOCK_GERAR_EVENTO_FALTANTE_EM_SELECAO_DE_DOCUMENTOS";

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    private DocumentoFiscalService documentoFiscalService;

    @Autowired
    private ConsultarProtocoloService consultarProtocoloService;

    @Autowired
    private LoteService loteService;

    @Scheduled(fixedDelay = 600_000L) //de 10 em 10 minutos
    public void gerarEventoFaltante() throws UnknownHostException {
        LOGGER.info("gerarEventoFaltante (scheduled) - INICIO");

        String statusScheduled = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_SCHEDULED_GERAR_EVENTO_FALTANTE);
        if (!"ON".equals(statusScheduled)) {
            throw new FazemuScheduledException("Chave de robo desligada");
        }

        String myHostLock = LoteUtils.getMyHost() + "_" + LOCK_HOST_EM_PROCESSAMENTO;

        if (cacheLoteRepository.setIfAbsent(myHostLock, true, 3, TimeUnit.MINUTES)) {

            List<DocumentoFiscal> listaDocumentosEleitos = getDocumentosElegiveis();

            listaDocumentosEleitos.stream().forEach(documentoFiscal -> {

                try {
                    consultarProtocoloService.atualizarConsultarProtocolo(documentoFiscal.getChaveAcessoEnviada(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), false);

                } catch (Exception e) {
                    LOGGER.error("gerarEventoFaltante (scheduled) - ERRO no documento fiscal {} {} ", documentoFiscal.getId(), e.getMessage(), e);
                }
            });

            // Excluir os eleitos do semaforo
            removerMembrosLista(listaDocumentosEleitos);

            redisOperationsService.expiresKey(myHostLock, 1L, TimeUnit.MILLISECONDS);
        }

        LOGGER.info("gerarEventoFaltante (scheduled) - FIM");
    }

    private List<DocumentoFiscal> getDocumentosElegiveis() {

        List<DocumentoFiscal> documentosElegiveis = Lists.newArrayList();

        if (cacheLoteRepository.setIfAbsent(LOCK_EM_SELECAO_DE_DOCUMENTOS, true, 1, TimeUnit.MINUTES)) {

            try {
                Set<Object> documentosEmProcessamento = redisOperationsService.members(KEY);

                String notIn = null;
                if (CollectionUtils.isNotEmpty(documentosEmProcessamento)) {
                    notIn = documentosEmProcessamento.stream().map(idDocFiscal -> String.valueOf(idDocFiscal)).collect(Collectors.joining(","));
                }

                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MINUTE, -parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_MIN_EMITIR_DOCUMENTOS_PARADOS, 10));

                Calendar cal2 = Calendar.getInstance();
                cal2.add(Calendar.MINUTE, -parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_MAX_EMITIR_DOCUMENTOS_PARADOS, 300));

                List<DocumentoFiscal> listaDocu = documentoFiscalService.listByDateIntervalAndSituacaoAndNotExistsDoev(TIPO_DOCUMENTO_FISCAL_NFE, cal.getTime(), cal2.getTime(), SituacaoEnum.LIQUIDADO, notIn);
                if (CollectionUtils.isNotEmpty(listaDocu)) {

                    // Limita o processo a quantidade pre estabelecida
                    int count = 0;
                    Integer docsPorProcesso = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_DOCUMENTOS_PROCESSADOS_EM_PARALELO, 30);

                    for (DocumentoFiscal documentoFiscal : listaDocu) {
                        //Verifica se o documento não esta ligado a um lote aberto/fechado/enviado
                        List<Lote> listaLotesAbertos = loteService.listByIdDocFiscalAndSituacao(documentoFiscal.getId(), SituacaoLoteEnum.ABERTO);
                        List<Lote> listaLotesFechados = loteService.listByIdDocFiscalAndSituacao(documentoFiscal.getId(), SituacaoLoteEnum.FECHADO);
                        List<Lote> listaLotesEnviados = loteService.listByIdDocFiscalAndSituacao(documentoFiscal.getId(), SituacaoLoteEnum.ENVIADO);

                        if (CollectionUtils.isEmpty(listaLotesAbertos) && CollectionUtils.isEmpty(listaLotesFechados) && CollectionUtils.isEmpty(listaLotesEnviados)) {
                            documentosElegiveis.add(documentoFiscal);

                            redisOperationsService.addToSet(KEY, documentoFiscal.getId());
                            count++;
                            if (count == docsPorProcesso) {
                                break;
                            }
                        }
                    }

                    if (CollectionUtils.isNotEmpty(documentosElegiveis)) {
                        redisOperationsService.expiresKey(KEY, 5L, TimeUnit.MINUTES);
                    }
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        redisOperationsService.expiresKey(LOCK_EM_SELECAO_DE_DOCUMENTOS, 1L, TimeUnit.MILLISECONDS);
        return documentosElegiveis;
    }

    private void removerMembrosLista(List<DocumentoFiscal> lista) {
        LOGGER.info("gerarEventoFaltante (scheduled) - removerMembrosLista {}", lista.size());

        lista.forEach((documento) -> {
            redisOperationsService.removeFromSet(KEY, documento.getId());
        });
    }
}
