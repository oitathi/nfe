package com.b2wdigital.fazemu.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.b2wdigital.fazemu.business.client.ConsultarReciboNFeClient;
import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.CodigoRetornoAutorizadorRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoEpecRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoEventoRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoLoteRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoRetornoRepository;
import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.repository.LoteEventoRepository;
import com.b2wdigital.fazemu.business.repository.LoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.repository.UrlRepository;
import com.b2wdigital.fazemu.business.service.EmitirLoteService;
import com.b2wdigital.fazemu.business.service.KafkaProducerService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEpec;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.Lote;
import com.b2wdigital.fazemu.domain.ResumoDocumentoFiscal;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.enumeration.CodigoRetornoAutorizadorEnum;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.PontoLoteEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;
import com.b2wdigital.fazemu.enumeration.TipoEmissaoEnum;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.utils.DateUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TConsReciNFe;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe.InfProt;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TRetConsReciNFe;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;

import br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NfeResultMsg;
import com.b2wdigital.fazemu.business.service.ConsultarReciboService;
import com.b2wdigital.fazemu.utils.FazemuUtils;

/**
 *
 * @author dailton.almeida
 */
@Service
public class ConsultarReciboServiceImpl implements ConsultarReciboService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarReciboServiceImpl.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private EmitirLoteService emitirLoteService;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private LoteEventoRepository loteEventoRepository;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private DocumentoEventoRepository documentoEventoRepository;

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoLoteRepository documentoLoteRepository;

    @Autowired
    private DocumentoRetornoRepository documentoRetornoRepository;

    @Autowired
    private DocumentoEpecRepository documentoEpecRepository;

    @Autowired
    private CodigoRetornoAutorizadorRepository codigoRetornoAutorizadorRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private ConsultarReciboNFeClient consultarReciboNFeClient;

    @Autowired
    @Qualifier("nfeRetAutorizacaoContext")
    private JAXBContext context;

    @Override
    public void consultarReciboCallback(ResumoLote lote) {
        LOGGER.info("ConsultarReciboServiceImpl >> consultarReciboCallback do lote {}", lote);
        Map<DocumentoFiscal, String> map = consultarRecibo(lote);

        try {

            for (Map.Entry<DocumentoFiscal, String> entry : map.entrySet()) {
                DocumentoFiscal docu = entry.getKey();
                String xmlProcessado = entry.getValue();
                kafkaProducerService.invokeCallback(docu, xmlProcessado, TipoServicoEnum.AUTORIZACAO);
                LOGGER.info("ConsultarReciboServiceImpl >> invokeCallback do documento fiscal {}", docu.getId());
            }
        } catch (Exception e) {
            LOGGER.error("ConsultarReciboServiceImpl >> Não foi possível produzir as mensagens de callback para o lote {} - ERRO {}", lote, e.getMessage());

            try {

                for (Map.Entry<DocumentoFiscal, String> entry : map.entrySet()) {
                    DocumentoFiscal docu = entry.getKey();
                    String xmlProcessado = entry.getValue();
                    kafkaProducerService.invokeCallback(docu, xmlProcessado, TipoServicoEnum.AUTORIZACAO);
                    LOGGER.info("ConsultarReciboServiceImpl >> invokeCallback do documento fiscal {}", docu.getId());
                }
            } catch (Exception e2) {
                LOGGER.error("ConsultarReciboServiceImpl >> Não foi possível produzir as mensagens de callback para o lote {} - ERRO 2 {}", lote, e2.getMessage());
            }
        }
    }

    @Override
    public Map<DocumentoFiscal, String> consultarRecibo(ResumoLote lote) {
        LOGGER.info("ConsultarReciboServiceImpl >> consultarRecibo do lote {}", lote);

        Long idLote = 0L;
        String usuario = StringUtils.EMPTY;
        Map<DocumentoFiscal, String> mapToCallback = Maps.newHashMap();
        try {
            idLote = lote.getIdLote();
            cacheLoteRepository.consultandoLote(idLote); //consultando recibo na sefaz
            //oracle nao precisa do equivalente do consultando a principio

            //obter url a partir do lote
            String url = urlRepository.getUrl(lote.getUf(), lote.getTipoEmissao(), ServicosEnum.RETORNO_AUTORIZACAO_NFE.getNome(), lote.getVersao());

            usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

            //montar mensagem do lote para sefaz
            TConsReciNFe tConsReciNFe = new TConsReciNFe();
            tConsReciNFe.setNRec(lote.getRecibo());
            tConsReciNFe.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
            tConsReciNFe.setVersao(lote.getVersao());

            Document docFinal = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            context.createMarshaller().marshal(tConsReciNFe, docFinal);

            // retira namespace
            docFinal = XMLUtils.cleanNameSpace(docFinal);

            // Atualiza documentos fiscais do lote
            lote.getIdDocFiscalList().forEach((idDocFiscal) -> {
                documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(idDocFiscal, null, PontoDocumentoEnum.CONSULTA_RECIBO, SituacaoEnum.ABERTO, null, null);
            });

            // Faz a integração com a SEFAZ
            NfeDadosMsg nfeDadosMsg = new NfeDadosMsg();
            nfeDadosMsg.getContent().add(docFinal.getDocumentElement());
            LOGGER.info("ConsultarReciboServiceImpl >> " + DateUtils.convertDateToString(new Date()) + " << LOTE {} | Enviando consulta de retorno de nfeAutorizacao ", lote);
            NfeResultMsg nfeResultMsg = consultarReciboNFeClient.nfeRetAutorizacaoLote(url, nfeDadosMsg, lote);
            LOGGER.info("ConsultarReciboServiceImpl >> " + DateUtils.convertDateToString(new Date()) + " << LOTE {} | Recebido consulta de retorno de nfeAutorizacao ", lote);

            //processa resultado
            List<Object> contentList = nfeResultMsg.getContent();
            Object content = contentList.get(0);
            LOGGER.debug("ConsultarReciboServiceImpl >> CONTENT LIST {} CONTENT {} CONTENT CLASS {}", contentList, content, content.getClass());
            TRetConsReciNFe tRetConsReciNFe = (TRetConsReciNFe) context.createUnmarshaller().unmarshal((Node) content);

            // xml lote
            Document doc = XMLUtils.createNewDocument();
            context.createMarshaller().marshal(tRetConsReciNFe, doc);

            String xmlLoteRetorno = XMLUtils.convertDocumentToString(doc);
            Long idClobRetorno = documentoClobRepository.insert(DocumentoClob.build(null, xmlLoteRetorno, usuario));

            // Atualiza data de ultima consulta
            lote.setDataUltimaConsultaRecibo(new Date());
            cacheLoteRepository.atualizarLote(lote);
            loteRepository.atualizarDataUltimaConsulta(idLote, lote.getDataUltimaConsultaRecibo(), usuario);

            Integer cStat = Integer.valueOf(tRetConsReciNFe.getCStat());
            LOGGER.info("ConsultarReciboServiceImpl >> CSTAT {} do LOTE {}", cStat, lote);

            String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(cStat);
            Boolean situacaoFinalizadora;

            if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())
                    || StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.REJEITADO_FINALIZADO.getCodigo())) {
                LOGGER.info("ConsultarReciboServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Sucesso na Consulta ", lote, cStat, situacaoAutorizacao);
                situacaoFinalizadora = true;

            } else if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.EM_PROCESSO.getCodigo())) {
                LOGGER.info("ConsultarReciboServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Em processo de Consulta ", lote, cStat, situacaoAutorizacao);
                situacaoFinalizadora = false;
            } else if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.REJEITADO_TRATAMENTO.getCodigo())) {
                LOGGER.info("ConsultarReciboServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Falha na Consulta ", lote, cStat, situacaoAutorizacao);
                situacaoFinalizadora = false;
            } else {
                LOGGER.error("ConsultarReciboServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Erro não mapeado na Consulta ", lote, cStat, situacaoAutorizacao);
                situacaoFinalizadora = false;
            }

            if (situacaoFinalizadora == true) {
                LOGGER.info("ConsultarReciboServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Processando a Consulta ", lote, cStat, situacaoAutorizacao);

                cacheLoteRepository.finalizarLote(idLote);
                loteRepository.finalizarLote(idLote, cStat, usuario);
                loteEventoRepository.insert(idLote, PontoLoteEnum.LIQUIDADO.getCodigo(), usuario, idClobRetorno, null);
                for (TProtNFe protNFe : ListUtils.emptyIfNull(tRetConsReciNFe.getProtNFe())) {
                    String chaveAcesso = protNFe.getInfProt().getChNFe();

                    DocumentoFiscal docu = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);
                    String xmlNFe = documentoFiscalRepository.getXmlByIdLoteAndIdDocFiscal(idLote, docu.getId());
                    String xmlProcessado = this.encaixarProtocolo(xmlNFe, protNFe, docu, usuario);

                    // Adiciona ao map do callback
                    mapToCallback.put(docu, xmlProcessado);

                }
                cacheLoteRepository.removerLoteFinalizado(idLote);
            } else {
                Integer countSituacaoAutorizacao = loteRepository.countSituacaoAutorizacaoById(idLote);
                LOGGER.info("ConsultarReciboServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Count na Consulta {} ", lote, cStat, situacaoAutorizacao, countSituacaoAutorizacao);

                if (countSituacaoAutorizacao == 0 || StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.EM_PROCESSO.getCodigo())) {
                    LOGGER.info("ConsultarReciboServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Em processo/Falha na Consulta ", lote, cStat, situacaoAutorizacao);
                    loteRepository.updateLoteSituacaoAutorizacao(idLote, cStat, usuario);
                    cacheLoteRepository.desconsultandoLote(idLote);

                    // Timer Task Retry de Consulta Lote
                    Integer tempoEspera = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_ESPERA_CONSULTA_RECIBO, 120); //default 2 minutos === 120 segundos

                    LOGGER.info("TIMER TASK DE RETRY DE CONSULTA DO LOTE {} - TEMPO DE ESPERA {} SEG.", lote.getIdLote(), tempoEspera);
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            consultarReciboCallback(lote);
                        }
                    };
                    Timer timer = new Timer();
                    timer.schedule(task, tempoEspera * 1000);

                } else {
                    LOGGER.error("ConsultarReciboServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Erro na Consulta ", lote, cStat, situacaoAutorizacao);

                    cacheLoteRepository.finalizarLote(idLote);
                    loteRepository.finalizarLote(idLote, cStat, usuario);
                    loteEventoRepository.insert(idLote, PontoLoteEnum.LIQUIDADO.getCodigo(), usuario, idClobRetorno, null);

                    Map<DocumentoFiscal, String> mapErrors = desmembrarEncerrarProcesso(lote, tRetConsReciNFe, usuario);
                    if (mapErrors.size() > 0) {
                        mapToCallback.put(mapErrors.entrySet().iterator().next().getKey(), mapErrors.entrySet().iterator().next().getValue());
                    }

                    cacheLoteRepository.removerLoteFinalizado(idLote);
                }
            }

        } catch (Exception e) {
            try {
                LOGGER.info("ConsultarReciboServiceImpl >> ERRO AO CONSULTAR LOTE {} {} ", lote, e.getMessage(), e);
                cacheLoteRepository.cancelarLoteConsultando(idLote);
                loteRepository.cancelarLote(idLote, usuario);
                loteEventoRepository.insert(idLote, PontoLoteEnum.CANCELADO.getCodigo(), usuario, null, null);
                cacheLoteRepository.removerLoteCancelado(idLote);

                // Logando evento de erro
                loteEventoRepository.insert(lote.getIdLote(), PontoLoteEnum.ERRO_CONSULTAR_LOTE.getCodigo(), usuario, null, e.getMessage());

                // Caso sejam EPEC, tira DPEC de Enviado para Aberto
                lote.getIdDocFiscalList()
                        .stream()
                        .filter((IdDocFiscal) -> (documentoEpecRepository.findByIdDocFiscalAndSituacao(IdDocFiscal, SituacaoEnum.ENVIADO) != null))
                        .forEachOrdered((IdDocFiscal) -> {
                            documentoEpecRepository.updateSituacao(IdDocFiscal, SituacaoEnum.ABERTO);
                        });

            } catch (Exception e2) {
                LOGGER.info("ConsultarReciboServiceImpl >> Exception da exception da consulta de recibos", e2);
                LOGGER.error(e2.getMessage(), e2);
            }
        }

        return mapToCallback;
    }

    protected Map<DocumentoFiscal, String> desmembrarEncerrarProcesso(ResumoLote lote, TRetConsReciNFe tRetConsReciNFe, String usuario) throws JsonProcessingException, Exception {
        LOGGER.info("ConsultarReciboServiceImpl >> desmembrarEncerrarProcesso ResumoLote {}, TRetConsReciNFe {} ", lote, tRetConsReciNFe);

        Map<DocumentoFiscal, String> mapToCallback = Maps.newHashMap();

        List<Long> idDocFiscalList = lote.getIdDocFiscalList();
        if (idDocFiscalList.size() > 1) {
            desmembrarLote(lote, true);

        } else if (tRetConsReciNFe != null) {
            if (CollectionUtils.isEmpty(tRetConsReciNFe.getProtNFe())) {
                Long idDocFiscal = idDocFiscalList.get(0);
                DocumentoFiscal docu = documentoFiscalRepository.findById(idDocFiscal);
                String xmlNFe = documentoFiscalRepository.getXmlByIdLoteAndIdDocFiscal(lote.getIdLote(), idDocFiscal);

                //constroi TProtNFe artificial com dados do retorno do lote e nao da NFe
                InfProt infProt = new InfProt();
                infProt.setTpAmb(tRetConsReciNFe.getTpAmb());
                infProt.setVerAplic(tRetConsReciNFe.getVerAplic());
                infProt.setChNFe(docu.getChaveAcesso());
                infProt.setDhRecbto(tRetConsReciNFe.getDhRecbto());
                infProt.setCStat(tRetConsReciNFe.getCStat()); //nesse cenario NFe ficara com cStat e xMotivo do recibo do lote e nao com o cStat da propria NFe na SeFaz como de praxe
                infProt.setXMotivo(tRetConsReciNFe.getXMotivo());
                TProtNFe protNFe = new TProtNFe();
                protNFe.setInfProt(infProt);

                String xmlProcessado = this.encaixarProtocolo(xmlNFe, protNFe, docu, usuario);

                // Adiciona ao map do callback
                mapToCallback.put(docu, xmlProcessado);

            } else {
                TProtNFe protNFe = tRetConsReciNFe.getProtNFe().get(0);
                String chaveAcesso = protNFe.getInfProt().getChNFe();

                DocumentoFiscal docu = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);

                String xmlNFe = documentoFiscalRepository.getXmlByIdLoteAndIdDocFiscal(lote.getIdLote(), docu.getId());

                String xmlProcessado = this.encaixarProtocolo(xmlNFe, protNFe, docu, usuario);

                // Adiciona ao map do callback
                mapToCallback.put(docu, xmlProcessado);
            }
        }
        return mapToCallback;
    }

    protected void desmembrarLote(ResumoLote lote, boolean isReprocessamento) {
        LOGGER.info("ConsultarReciboServiceImpl >> LOTE {} | desmembrarLote", lote.getIdLote());

        List<Long> idDocFiscalList = lote.getIdDocFiscalList();
        idDocFiscalList
                .stream()
                .map(idDocFiscal -> {
                    DocumentoClob docl = documentoClobRepository.findByIdLoteAndIdDocFiscal(lote.getIdLote(), idDocFiscal);
                    return ResumoDocumentoFiscal.build(idDocFiscal, lote.getTipoDocumentoFiscal(), lote.getIdEmissor(), lote.getUf(), lote.getMunicipio(), lote.getTipoEmissao(), lote.getVersao(), docl.getId(), docl.getClob().length());
                })
                .forEach(resumoDocumentoFiscal -> {
                    emitirLoteService.emitirLote(resumoDocumentoFiscal, ServicosEnum.getByName(lote.getServico()), true);
                });
    }

    @Override
    public String encaixarProtocolo(String xmlNFe, TProtNFe protNFe, DocumentoFiscal docu, String usuario) throws JAXBException {
        LOGGER.info("ConsultarReciboServiceImpl >> encaixarProtocolo {} ", docu.getId());
        TNFe tNFe = (TNFe) context.createUnmarshaller().unmarshal(new StringSource(xmlNFe));

        TNfeProc nfeProc = new TNfeProc();
        nfeProc.setNFe(tNFe);
        nfeProc.setProtNFe(protNFe);
        nfeProc.setVersao(protNFe.getVersao());

        StringResult stringResult = new StringResult();
        context.createMarshaller().marshal(nfeProc, stringResult);
        String result = stringResult.toString();

        try {
            Document docResult = XMLUtils.convertStringToDocument(result);
            docResult = XMLUtils.cleanNameSpace(docResult);
            result = XMLUtils.convertDocumentToString(docResult);
        } catch (Exception e) {
            return null;
        }

        Long idClob = documentoClobRepository.insert(DocumentoClob.build(null, result, usuario));
        documentoEventoRepository.insert(DocumentoEvento.build(null, docu.getId(), PontoDocumentoEnum.PROCESSADO.getCodigo(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), protNFe.getInfProt().getCStat(), idClob, usuario));

        String situacaoDocumento = docu.getSituacaoDocumento();
        String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(Integer.valueOf(protNFe.getInfProt().getCStat()));
        if ("110".equals(protNFe.getInfProt().getCStat())
                || "301".equals(protNFe.getInfProt().getCStat())
                || "302".equals(protNFe.getInfProt().getCStat())
                || "303".equals(protNFe.getInfProt().getCStat())) {
            situacaoDocumento = SituacaoDocumentoEnum.DENEGADO.getCodigo();

        } else if ((SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                || SituacaoDocumentoEnum.REJEITADO.getCodigo().equals(situacaoDocumento))
                && CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
            situacaoDocumento = SituacaoDocumentoEnum.AUTORIZADO.getCodigo();

        } else if ((SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                || (SituacaoDocumentoEnum.AUTORIZADO.getCodigo().equals(situacaoDocumento)
                && TipoEmissaoEnum.EPEC.getCodigo().toString().equals(nfeProc.getNFe().getInfNFe().getIde().getTpEmis())
                && !"468".equals(protNFe.getInfProt().getCStat())))
                && !CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
            situacaoDocumento = SituacaoDocumentoEnum.REJEITADO.getCodigo();
        }

        documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(docu.getId(), protNFe.getInfProt().getCStat(), PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, Long.valueOf(tNFe.getInfNFe().getIde().getTpEmis()), situacaoDocumento);

        //Grava a documento retorno apenas quando documento foi processado com sucesso
        if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())) {
            if (documentoRetornoRepository.findByIdDocFiscalAndTpServicoAndTpEvento(docu.getId(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null) == null) {
                documentoRetornoRepository.insert(docu.getId(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null, idClob, usuario, usuario);
            } else {
                documentoRetornoRepository.update(docu.getId(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null, idClob, usuario);
            }
        }

        finalizarEpec(docu, protNFe.getInfProt().getCStat());

        return result;
    }

    private void finalizarEpec(DocumentoFiscal docu, String cstat) {

        DocumentoEpec documentoEpec = documentoEpecRepository.findByIdDocumentoFiscalAndIdEstado(docu.getId(), null);
        if (documentoEpec != null) {
            // Caso nota não tenha sido conciliada, atualiza datahora
            if ("468".equals(cstat)) {
                LOGGER.info("ConsultarReciboServiceImpl >> atualiza data EPEC {} ", docu.getId());
                documentoEpecRepository.updateSituacao(docu.getId(), SituacaoEnum.ABERTO);
            } else {
                LOGGER.info("ConsultarReciboServiceImpl >> finaliza EPEC {} ", docu.getId());
                documentoEpecRepository.updateSituacao(docu.getId(), SituacaoEnum.LIQUIDADO);
            }
        }
    }

    @Override
    public void reconstruirLotesEnviados() {
        LOGGER.info("reconstruirLotesEnviados");
        List<Lote> loteList = loteRepository.obterLotesPorSituacao(SituacaoLoteEnum.ENVIADO.getCodigo());
        if (CollectionUtils.isNotEmpty(loteList)) {
            long currentTimeMillis = System.currentTimeMillis();
            String key = "lotesEnviados" + currentTimeMillis;
            loteList.stream().map((lote) -> {
                LOGGER.debug("reconstruirLotesEnviados lote {}", lote.getId());
                return lote;
            }).forEachOrdered((lote) -> {
                ResumoLote resumoLote = ResumoLote.fromLote(lote, documentoLoteRepository.listByIdDocFiscal(lote.getId()));
                resumoLote.setUf(estadoRepository.findById(lote.getIdEstado()).getCodigoIbge());
                resumoLote.setDataAbertura(new Date());

                cacheLoteRepository.atualizarLote(resumoLote);
                cacheLoteRepository.adicionarLoteEnviado(lote.getId());
            });
            cacheLoteRepository.sobreporLotesEnviados(key);
        }
    }

}
