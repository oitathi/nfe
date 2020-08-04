package com.b2wdigital.fazemu.scheduled;

import com.b2wdigital.assinatura_digital.CertificadoDigital;
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
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.business.service.DocumentoClobService;
import com.b2wdigital.fazemu.business.service.DocumentoEpecService;
import com.b2wdigital.fazemu.business.service.DocumentoFiscalService;
import com.b2wdigital.fazemu.business.service.EmitirLoteService;
import com.b2wdigital.fazemu.business.service.EstadoService;
import com.b2wdigital.fazemu.business.service.LoteService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.business.service.TipoEmissaoService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEpec;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.domain.Lote;
import com.b2wdigital.fazemu.domain.ResumoDocumentoFiscal;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.RecepcaoEventoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;
import com.b2wdigital.fazemu.enumeration.TipoEmissaoEnum;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.FazemuScheduledException;
import com.b2wdigital.fazemu.integration.dao.redis.RedisOperationsDao;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.LoteUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TRetConsSitNFe;
import com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TEvento;
import com.google.common.collect.Lists;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.w3c.dom.Document;

/**
 * Emitir Lote Documentos Parados.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class EmitirLoteDocumentosParadosScheduled extends AbstractScheduled {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmitirLoteDocumentosParadosScheduled.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    private static final String KEY = RedisOperationsDao.KEY_SEMAFORO_REEMITIR_LOTE_DOCUMENTOS_ABERTOS;
    private static final String LOCK_HOST_EM_PROCESSAMENTO = "LOCK_REEMITIR_LOTE_DOCUMENTOS_ABERTOS";
    private static final String LOCK_EM_SELECAO_DE_DOCUMENTOS = "LOCK_REEMITIR_LOTE_DOCUMENTOS_ABERTOS_EM_SELECAO_DE_DOCUMENTOS";

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    protected TipoEmissaoService tipoEmissaoService;

    @Autowired
    private DocumentoFiscalService documentoFiscalService;

    @Autowired
    private DocumentoEpecService documentoEpecService;

    @Autowired
    private DocumentoClobService documentoClobService;

    @Autowired
    private EstadoService estadoService;

    @Autowired
    private EmitirLoteService emitirLoteService;

    @Autowired
    private LoteService loteService;

    @Autowired
    @Qualifier("nfeRecepcaoEventoManifestacaoContext")
    private JAXBContext contextManifestacao;

    @Scheduled(fixedRate = 180_000L) //de 3 em 3 minutos
    public void emitirLoteDocumentosParados() throws UnknownHostException {
        LOGGER.info("emitirLoteDocumentosParados (scheduled) - INICIO");

        try {
            String statusScheduled = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_SCHEDULED_EMITIR_LOTE_DOCUMENTOS_PARADOS);
            if (!"ON".equals(statusScheduled)) {
                throw new FazemuScheduledException("Chave de robo desligada");
            }

            String myHostLock = LoteUtils.getMyHost() + "_" + LOCK_HOST_EM_PROCESSAMENTO;

            if (cacheLoteRepository.setIfAbsent(myHostLock, true, 3, TimeUnit.MINUTES)) {

                // Verifica todos os estados ativos
                List<Estado> listaEstadoAtivo = estadoService.listAll();

                listaEstadoAtivo.stream().forEach(estado -> {
                    LOGGER.info("emitirLoteDocumentosParados (scheduled) - estado {}", estado.getNome());

                    // Somente faz se o tipo de emissao atual for NORMAL para o estado
                    Integer tipoEmissaoAtual = tipoEmissaoService.getTipoEmissaoByCodigoIBGE(String.valueOf(estado.getCodigoIbge()));

                    if (TipoEmissaoEnum.NORMAL.getCodigo().equals(tipoEmissaoAtual)) {
                        LOGGER.info("emitirLoteDocumentosParados (scheduled) - tipoEmissaoAtual {}", tipoEmissaoAtual);

                        List<DocumentoFiscal> listaDocumentosEleitos = getDocumentosElegiveisByEstado(estado);

                        listaDocumentosEleitos.stream().forEach(documentoFiscal -> {

                            try {
                                //Tem essa verificacao por conta das inutilizacoes
                                if (documentoFiscal.getChaveAcesso() != null) {

                                    if (!PontoDocumentoEnum.RECEBIDO_XML_MANIFESTACAO.getCodigo().equals(documentoFiscal.getIdPonto())
                                            && !PontoDocumentoEnum.MANIFESTACAO.getCodigo().equals(documentoFiscal.getIdPonto())) {

                                        //Valida se a emissão é de EPEC
                                        if (Long.valueOf(TipoEmissaoEnum.EPEC.getCodigo()).equals(documentoFiscal.getTipoEmissao())) {
                                            try {
                                                DocumentoEpec documentoEpec = documentoEpecService.findByIdDocumentoFiscalAndIdEstado(documentoFiscal.getId(), documentoFiscal.getIdEstado());

                                                //Validar se realmente foi emitida antes
                                                if (documentoEpec == null) {
                                                    assinarDocumento(documentoFiscal);
                                                }
                                            } catch (Exception e) {
                                                assinarDocumento(documentoFiscal);
                                            }
                                        }

                                        TRetConsSitNFe tRetConsSitNFe = consultarProtocolo(documentoFiscal);
                                        LOGGER.info("emitirLoteDocumentosParados (scheduled) - consultarProtocolo {}", documentoFiscal.getId());

                                        if (tRetConsSitNFe != null) {
                                            LOGGER.info("emitirLoteDocumentosParados (scheduled) - consultarProtocolo  {} CSTAT {}", documentoFiscal.getId(), tRetConsSitNFe.getCStat());
                                            // Rejeição: NF-e não consta na base de dados da SEFAZ
                                            if ("217".equals(tRetConsSitNFe.getCStat())) {

                                                if (!PontoDocumentoEnum.INUTILIZACAO.getCodigo().equals(documentoFiscal.getIdPonto())) {
                                                    LOGGER.info("emitirLoteDocumentosParados (scheduled) - Emitir Lote de Autorizacao {}", documentoFiscal.getId());

                                                    DocumentoClob documentoClob = documentoClobService.getLastXmlSignedByIdDocFiscal(documentoFiscal.getId());

                                                    if (documentoClob == null) {
                                                        documentoClob = assinarDocumento(documentoFiscal);
                                                    }

                                                    emitirNovoLote(tRetConsSitNFe, documentoFiscal, tipoEmissaoAtual, ServicosEnum.AUTORIZACAO_NFE, documentoClob);
                                                }

                                                // Caso esteja CANCELADO sem processar protocolo
                                            } else if ("101".equals(tRetConsSitNFe.getCStat()) || tRetConsSitNFe.getRetCancNFe() != null) {
                                                LOGGER.info("emitirLoteDocumentosParados (scheduled) - Processar Retorno de Cancelamento {}", documentoFiscal.getId());
                                                processarDocumentosRecepcaoEvento(tRetConsSitNFe, documentoFiscal, TipoServicoEnum.CANCELAMENTO);

                                                // Caso tenha tido uma tentativa de CANCELAMENTO porém sem efetivar na SEFAZ
                                            } else if (PontoDocumentoEnum.CANCELAMENTO.getCodigo().equals(documentoFiscal.getIdPonto())) {
                                                LOGGER.info("emitirLoteDocumentosParados (scheduled) - Emitir Lote de Cancelamento {}", documentoFiscal.getId());

                                                DocumentoClob docClob = documentoClobService.getLastXmlByIdDocFiscalAndPontoDocumento(documentoFiscal.getId(), PontoDocumentoEnum.getByCodigo(documentoFiscal.getIdPonto()));
                                                emitirNovoLote(tRetConsSitNFe, documentoFiscal, tipoEmissaoAtual, ServicosEnum.RECEPCAO_EVENTO_CANCELAMENTO, docClob);

                                                // Caso tenha tido uma tentativa de CARTA DE CORRECAO porém sem efetivar na SEFAZ
                                            } else if (PontoDocumentoEnum.CARTA_CORRECAO.getCodigo().equals(documentoFiscal.getIdPonto())) {
                                                LOGGER.info("emitirLoteDocumentosParados (scheduled) - Emitir Lote de Carta de Correcao {}", documentoFiscal.getId());

                                                DocumentoClob docClob = documentoClobService.getLastXmlByIdDocFiscalAndPontoDocumento(documentoFiscal.getId(), PontoDocumentoEnum.getByCodigo(documentoFiscal.getIdPonto()));
                                                emitirNovoLote(tRetConsSitNFe, documentoFiscal, tipoEmissaoAtual, ServicosEnum.RECEPCAO_EVENTO_CARTA_CORRECAO, docClob);

                                                // Caso tenha tido uma tentativa de INUTILIZACAO porém sem efetivar na SEFAZ
                                            } else if (PontoDocumentoEnum.INUTILIZACAO.getCodigo().equals(documentoFiscal.getIdPonto())) {
                                                LOGGER.info("emitirLoteDocumentosParados (scheduled) - Emitir Lote de Inutilizacao {}", documentoFiscal.getId());

                                                DocumentoClob docClob = documentoClobService.getLastXmlByIdDocFiscalAndPontoDocumento(documentoFiscal.getId(), PontoDocumentoEnum.getByCodigo(documentoFiscal.getIdPonto()));
                                                emitirNovoLote(tRetConsSitNFe, documentoFiscal, tipoEmissaoAtual, ServicosEnum.INUTILIZACAO, docClob);

                                                // Caso tenha tido informacao de EPEC Autorizado, ajustar para que processo seja feito pelo Schedule de EPEC e nao de documentos parados
                                            } else if ("124".equals(tRetConsSitNFe.getCStat())) {
                                                LOGGER.info("emitirLoteDocumentosParados (scheduled) - EPEC Autorizado {}", documentoFiscal.getId());

                                                // Caso sejam EPEC, atualiza DPEC de Enviado para Aberto e Liquida Documento Fiscal
                                                if (documentoEpecService.findByIdDocFiscalAndSituacao(documentoFiscal.getId(), SituacaoEnum.ENVIADO) != null) {
                                                    documentoEpecService.updateSituacao(documentoFiscal.getId(), SituacaoEnum.ABERTO);
                                                    documentoFiscalService.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(documentoFiscal.getId(), documentoFiscal.getSituacaoAutorizador(), PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, null, null);

                                                } else if (documentoEpecService.findByIdDocFiscalAndSituacao(documentoFiscal.getId(), SituacaoEnum.ABERTO) != null
                                                        && SituacaoEnum.ABERTO.getCodigo().equals(documentoFiscalService.findById(documentoFiscal.getId()).getSituacao())) {
                                                    documentoFiscalService.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(documentoFiscal.getId(), documentoFiscal.getSituacaoAutorizador(), PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, null, null);
                                                }

                                                // Caso tenha tido erro de Consumo Indevido, atualiza a datahora para não entrar na lista de documentos elegiveis
                                            } else if ("656".equals(tRetConsSitNFe.getCStat())) {
                                                LOGGER.info("emitirLoteDocumentosParados (scheduled) - Consumo Indevido {}", documentoFiscal.getId());

                                                Calendar cal = Calendar.getInstance();
                                                cal.add(Calendar.MINUTE, parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_INTERVALO_CONSUMO_INDEVIDO, 60));
                                                documentoFiscalService.updateDataHora(documentoFiscal.getId(), cal.getTime());
                                            } else {
                                                LOGGER.info("emitirLoteDocumentosParados (scheduled) - Processar Retorno de Autorizacao {}", documentoFiscal.getId());
                                                processarProtocoloAutorizacao(tRetConsSitNFe, documentoFiscal);
                                            }

                                        }
                                    } else {
                                        DocumentoClob documentoClob = null;
                                        Document docNFSe = null;
                                        if (PontoDocumentoEnum.RECEBIDO_XML_MANIFESTACAO.getCodigo().equals(documentoFiscal.getIdPonto())) {
                                            documentoClob = documentoClobService.getLastXmlByIdDocFiscalAndPontoDocumento(documentoFiscal.getId(), PontoDocumentoEnum.RECEBIDO_XML_MANIFESTACAO);

                                            docNFSe = XMLUtils.convertStringToDocument(documentoClob.getClob());

                                            // Certificado
                                            long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(Long.valueOf(documentoFiscal.getIdDestinatario()));
                                            CertificadoDigital certificado = certificadoDigitalService.getByIdEmissorRaiz(raizCnpjEmitente);

                                            FazemuUtils.signXml(docNFSe, certificado, ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO);

                                        } else if (PontoDocumentoEnum.MANIFESTACAO.getCodigo().equals(documentoFiscal.getIdPonto())) {
                                            documentoClob = documentoClobService.getLastXmlByIdDocFiscalAndPontoDocumento(documentoFiscal.getId(), PontoDocumentoEnum.MANIFESTACAO);
                                            docNFSe = XMLUtils.convertStringToDocument(documentoClob.getClob());
                                        }

                                        if (documentoClob != null) {

                                            // Unmarshal
                                            TEvento tEvento = (TEvento) contextManifestacao.createUnmarshaller().unmarshal(docNFSe);

//                                            PontoDocumentoEnum pontoDocumento = PontoDocumentoEnum.MANIFESTACAO;
                                            Integer tipoEmissao = parametrosInfraRepository.getAsInteger(null, ParametrosInfraRepository.PAIN_TP_EMISSAO);
                                            ServicosEnum servico = null;
                                            Integer tipoEvento = Integer.valueOf(tEvento.getInfEvento().getTpEvento());
                                            if (RecepcaoEventoEnum.MANIFESTACAO_CONFIRMACAO_OPERACAO.getCodigoEvento().equals(tipoEvento)
                                                    || RecepcaoEventoEnum.MANIFESTACAO_CIENCIA_OPERACAO.getCodigoEvento().equals(tipoEvento)
                                                    || RecepcaoEventoEnum.MANIFESTACAO_DESCONHECIMENTO_OPERACAO.getCodigoEvento().equals(tipoEvento)
                                                    || RecepcaoEventoEnum.MANIFESTACAO_OPERACAO_NAO_REALIZADA.getCodigoEvento().equals(tipoEvento)) {
                                                servico = ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO;
                                            }

                                            emitirLoteService.emitirLote(ResumoDocumentoFiscal.build(documentoFiscal.getId(),
                                                    documentoFiscal.getTipoDocumentoFiscal(),
                                                    Long.valueOf(tEvento.getInfEvento().getCNPJ()),
                                                    Integer.valueOf(tEvento.getInfEvento().getCOrgao()),
                                                    null,
                                                    tipoEmissao,
                                                    servico.getVersao(),
                                                    documentoClob.getId(),
                                                    documentoClob.getClob().length()),
                                                    servico,
                                                    false);
                                        }
                                    }
                                }

                            } catch (Exception e) {
                                LOGGER.error("emitirLoteDocumentosParados (scheduled) - ERRO no documento fiscal {} {} ", documentoFiscal.getId(), e.getMessage(), e);
                            }
                        });

                        // Excluir os eleitos do semaforo
                        removerMembrosLista(listaDocumentosEleitos);

                        LOGGER.info("emitirLoteDocumentosParados (scheduled) - Removendo documentos do semáforo para estado {}", estado.getId());
                    }

                });

                redisOperationsService.expiresKey(myHostLock, 1L, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            LOGGER.error("emitirLoteDocumentosParados (scheduled) - ERRO de execucao {} ", e.getMessage(), e);
        }

        LOGGER.info("emitirLoteDocumentosParados (scheduled) - FIM");
    }

    private List<DocumentoFiscal> getDocumentosElegiveisByEstado(Estado estado) {

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

                // Limita o processo a quantidade pre estabelecida
                int count = 0;
                Integer docsPorProcesso = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_DOCUMENTOS_PROCESSADOS_EM_PARALELO, 30);

                try {
                    List<DocumentoFiscal> listaDocumentoFiscalBySituacao = documentoFiscalService.listByDateIntervalAndIdEstadoAndSituacao(TIPO_DOCUMENTO_FISCAL_NFE, cal.getTime(), cal2.getTime(), estado.getId(), SituacaoEnum.ABERTO, notIn);
                    if (CollectionUtils.isNotEmpty(listaDocumentoFiscalBySituacao)) {

                        for (DocumentoFiscal documentoFiscal : listaDocumentoFiscalBySituacao) {
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
                    }
                } catch (Exception e) {
                    LOGGER.error("emitirLoteDocumentosParados (scheduled) - getDocumentosElegiveisByEstado - ERRO listByDateIntervalAndIdEstadoAndSituacao -  {} ", e.getMessage(), e);
                }

                try {
                    List<DocumentoFiscal> listaDocumentoFiscalBySituacaoAutorizacao = documentoFiscalService.listByDateIntervalAndIdEstadoAndSituacaoAutorizacao(TIPO_DOCUMENTO_FISCAL_NFE, cal.getTime(), cal2.getTime(), estado.getId(), notIn);
                    if (CollectionUtils.isNotEmpty(listaDocumentoFiscalBySituacaoAutorizacao)) {

                        for (DocumentoFiscal documentoFiscal : listaDocumentoFiscalBySituacaoAutorizacao) {
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
                    }
                } catch (Exception e) {
                    LOGGER.error("emitirLoteDocumentosParados (scheduled) - getDocumentosElegiveisByEstado - ERRO listByDateIntervalAndIdEstadoAndSituacaoAutorizacao -  {} ", e.getMessage(), e);
                }

                if (CollectionUtils.isNotEmpty(documentosElegiveis)) {
                    redisOperationsService.expiresKey(KEY, 5L, TimeUnit.MINUTES);
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        redisOperationsService.expiresKey(LOCK_EM_SELECAO_DE_DOCUMENTOS, 1L, TimeUnit.MILLISECONDS);
        return documentosElegiveis;
    }

    private void removerMembrosLista(List<DocumentoFiscal> lista) {
        LOGGER.info("emitirLoteDocumentosParados (scheduled) - removerMembrosLista {}", lista.size());

        lista.forEach((documento) -> {
            redisOperationsService.removeFromSet(KEY, documento.getId());
        });
    }

}
