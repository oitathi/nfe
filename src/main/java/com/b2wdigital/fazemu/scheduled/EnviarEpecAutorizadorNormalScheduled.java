package com.b2wdigital.fazemu.scheduled;

import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.DocumentoClobService;
import com.b2wdigital.fazemu.business.service.DocumentoEpecService;
import com.b2wdigital.fazemu.business.service.DocumentoEventoService;
import com.b2wdigital.fazemu.business.service.DocumentoFiscalService;
import com.b2wdigital.fazemu.business.service.EmitirLoteService;
import com.b2wdigital.fazemu.business.service.EstadoService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEpec;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.domain.ResumoDocumentoFiscal;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;
import com.b2wdigital.fazemu.enumeration.TipoEmissaoEnum;
import com.b2wdigital.fazemu.exception.FazemuScheduledException;
import com.b2wdigital.fazemu.integration.dao.redis.RedisOperationsDao;
import com.b2wdigital.fazemu.service.impl.AbstractNFeServiceImpl;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.LoteUtils;
import com.google.common.collect.Lists;

/**
 * Enviar Epec Autorizador Normal.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class EnviarEpecAutorizadorNormalScheduled extends AbstractNFeServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnviarEpecAutorizadorNormalScheduled.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    private static final String KEY = RedisOperationsDao.KEY_SEMAFORO_ENVIAR_EPEC_AUTORIZADOR_NORMAL;
    private static final String LOCK_HOST_EM_PROCESSAMENTO = "LOCK_ENVIAR_EPEC_AUTORIZADOR_NORMAL";
    private static final String LOCK_EM_SELECAO_DE_DOCUMENTOS = "LOCK_ENVIAR_EPEC_AUTORIZADOR_NORMAL_EM_SELECAO_DE_DOCUMENTOS";

    @Autowired
    private DocumentoFiscalService documentoFiscalService;

    @Autowired
    private DocumentoEventoService documentoEventoService;

    @Autowired
    private DocumentoClobService documentoClobService;

    @Autowired
    private DocumentoEpecService documentoEpecService;

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private EmitirLoteService emitirLoteService;

    @Autowired
    private EstadoService estadoService;

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Scheduled(fixedDelay = 180_000L) //de 3 em 3 minutos
    public void enviarEpecAutorizadorNormal() throws UnknownHostException {
        LOGGER.info("enviarEpecAutorizadorNormal (scheduled) - INICIO");

        String statusScheduled = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_SCHEDULED_ENVIAR_EPEC_AUTORIZADOR_NORMAL);
        if (!"ON".equals(statusScheduled)) {
            throw new FazemuScheduledException("Chave de robo desligada");
        }

        String myHostLock = LoteUtils.getMyHost() + "_" + LOCK_HOST_EM_PROCESSAMENTO;

        if (cacheLoteRepository.setIfAbsent(myHostLock, true, 3, TimeUnit.MINUTES)) {

            // Verifica todos os estados ativos
            List<Estado> listaEstadoAtivo = estadoService.listByAtivo();

            listaEstadoAtivo.stream().forEach(estado -> {
                LOGGER.info("enviarEpecAutorizadorNormal (scheduled) - estado {}", estado.getNome());

                //Verifica o tipo de emissao corrente
                Integer tipoEmissaoAtual = getTipoEmissao(estado.getCodigoIbge().toString());

                try {
                    if (TipoEmissaoEnum.NORMAL.getCodigo().equals(tipoEmissaoAtual)) {
                        LOGGER.info("enviarEpecAutorizadorNormal (scheduled) - tipoEmissaoAtual {}", tipoEmissaoAtual);

                        List<DocumentoEpec> listaDocumentosEleitos = getDocumentosElegiveisByEstado(estado);

                        if (listaDocumentosEleitos.size() > 0) {
                            LOGGER.info("enviarEpecAutorizadorNormal (scheduled) - listaDocumentosEleitos {}", listaDocumentosEleitos);

                            for (DocumentoEpec documentoEpec : listaDocumentosEleitos) {
                                try {
                                    DocumentoFiscal documentoFiscal = documentoFiscalService.findById(documentoEpec.getIdDocumentoFiscal());
                                    if (documentoFiscal != null) {
                                        LOGGER.info("enviarEpecAutorizadorNormal (scheduled) - Enviando para autorizador normal apos Epec documento {} ", documentoFiscal.getId());

                                        DocumentoClob docClob = documentoClobService.getLastXmlSignedByIdDocFiscal(documentoFiscal.getId());

                                        documentoFiscalService.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(documentoFiscal.getId(), null, PontoDocumentoEnum.REENVIO_NORMAL_APOS_EPEC, SituacaoEnum.ABERTO, null, null);

                                        String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);
                                        documentoEventoService.insert(DocumentoEvento.build(null, documentoFiscal.getId(), PontoDocumentoEnum.REENVIO_NORMAL_APOS_EPEC.getCodigo(), null, null, docClob.getId(), usuario));

                                        emitirLoteService.emitirLote(ResumoDocumentoFiscal.build(documentoFiscal.getId(),
                                                documentoFiscal.getTipoDocumentoFiscal(),
                                                documentoFiscal.getIdEmissor(),
                                                estado.getCodigoIbge(),
                                                documentoFiscal.getIdMunicipio(),
                                                tipoEmissaoAtual,
                                                ServicosEnum.AUTORIZACAO_NFE.getVersao(),
                                                docClob.getId(),
                                                docClob.getClob().length()),
                                                ServicosEnum.AUTORIZACAO_NFE,
                                                false);
                                    }

                                } catch (Exception e) {
                                    LOGGER.error("enviarEpecAutorizadorNormal (scheduled) exception {} documentoEpec {} ", e.getMessage(), e, documentoEpec);
                                }

                            }
                        }

                        // Excluir os eleitos do semaforo
                        removerMembrosLista(listaDocumentosEleitos);
                    }

                } catch (Exception e) {
                    LOGGER.error("enviarEpecAutorizadorNormal (scheduled) exception {} estado {}", e.getMessage(), e, estado.getNome());
                }

            });

            redisOperationsService.expiresKey(myHostLock, 1L, TimeUnit.MILLISECONDS);
        }
        
        LOGGER.info("enviarEpecAutorizadorNormal (scheduled) - FIM");
    }

    private List<DocumentoEpec> getDocumentosElegiveisByEstado(Estado estado) {

        List<DocumentoEpec> documentosElegiveis = Lists.newArrayList();

        if (cacheLoteRepository.setIfAbsent(LOCK_EM_SELECAO_DE_DOCUMENTOS, true, 1, TimeUnit.MINUTES)) {

            try {
                Set<Object> documentosEmProcessamento = redisOperationsService.members(KEY);

                String notIn = null;
                if (CollectionUtils.isNotEmpty(documentosEmProcessamento)) {
                    notIn = documentosEmProcessamento.stream().map(idDocFiscal -> String.valueOf(idDocFiscal)).collect(Collectors.joining(","));
                }

                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MINUTE, -parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_MIN_ENVIAR_EPEC_AUTORIZADOR_NORMAL, 10));

                List<DocumentoEpec> documentosAProcessar = documentoEpecService.listBySituacaoAndIdEstadoAndDataInicio(TIPO_DOCUMENTO_FISCAL_NFE, SituacaoEnum.ABERTO, estado.getId(), cal.getTime(), notIn);
                if (CollectionUtils.isNotEmpty(documentosAProcessar)) {

                    // Limita o processo a quantidade pre estabelecida
                    int count = 0;
                    Integer docsPorProcesso = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_DOCUMENTOS_PROCESSADOS_EM_PARALELO, 30);
                    for (DocumentoEpec documentoEpec : documentosAProcessar) {
                        documentosElegiveis.add(documentoEpec);
                        documentoEpecService.updateSituacao(documentoEpec.getIdDocumentoFiscal(), SituacaoEnum.ENVIADO);
                        redisOperationsService.addToSet(KEY, documentoEpec.getIdDocumentoFiscal());
                        count++;
                        if (count == docsPorProcesso) {
                            break;
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

    protected void removerMembrosLista(List<DocumentoEpec> lista) {
        LOGGER.info("enviarEpecAutorizadorNormal (scheduled) - removerMembrosLista {}", lista.size());

        lista.forEach((item) -> {
            redisOperationsService.removeFromSet(KEY, item.getIdDocumentoFiscal());
        });
    }

}
