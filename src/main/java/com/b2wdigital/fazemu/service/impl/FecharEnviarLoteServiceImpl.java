package com.b2wdigital.fazemu.service.impl;

import com.b2wdigital.assinatura_digital.CertificadoDigital;
import com.b2wdigital.assinatura_digital.exception.AssinaturaDigitalException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.b2wdigital.fazemu.business.client.AutorizacaoNFeClient;
import com.b2wdigital.fazemu.business.client.InutilizacaoNFeClient;
import com.b2wdigital.fazemu.business.client.RecepcaoEventoClient;
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
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.business.service.ConsultarReciboService;
import com.b2wdigital.fazemu.business.service.EmitirLoteService;
import com.b2wdigital.fazemu.business.service.FecharEnviarLoteService;
import com.b2wdigital.fazemu.business.service.ImpressaoNFeService;
import com.b2wdigital.fazemu.business.service.KafkaProducerService;
import com.b2wdigital.fazemu.business.service.ProcessarRetornoCartaCorrecaoService;
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
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;
import com.b2wdigital.fazemu.utils.DateUtils;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe;
import com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TRetEnviNFe;
import com.b2wdigital.nfe.schema.v4v160b.epec.EPEC_v1.TRetEnvEvento;
import com.b2wdigital.nfe.schema.v4v160b.epec.EPEC_v1.TRetEvento;
import com.b2wdigital.nfe.schema.v4v160b.inutNFe_v4.TRetInutNFe;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import javax.xml.transform.TransformerException;

/**
 *
 * @author dailton.almeida
 */
@SuppressWarnings("deprecation")
@Service
public class FecharEnviarLoteServiceImpl implements FecharEnviarLoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FecharEnviarLoteServiceImpl.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    private static final String XML_HEADER = "<?xml version='1.0' encoding='UTF-8'?>".replace('\'', '"'); //usa replace para nao ter que escapar tantas aspas
    private static final String ENVI_INI = "<enviNFe xmlns='http://www.portalfiscal.inf.br/nfe' versao='${versao}'><idLote>${idLote}</idLote><indSinc>${indSinc}</indSinc>".replace('\'', '"');
    private static final String ENVI_END = "</enviNFe>";

    private static final String ENV_EVENTO_INI = "<envEvento xmlns='http://www.portalfiscal.inf.br/nfe' versao='${versao}'><idLote>${idLote}</idLote>".replace('\'', '"');
    private static final String ENV_EVENTO_END = "</envEvento>";

    private static final String ID_SINC_ASSINCRONO = "0";

    @Autowired
    private EmitirLoteService emitirLoteService;

    @Autowired
    private ProcessarRetornoCartaCorrecaoService processarRetornoCartaCorrecaoService;

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private LoteEventoRepository loteEventoRepository;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private DocumentoLoteRepository documentoLoteRepository;

    @Autowired
    private DocumentoEventoRepository documentoEventoRepository;

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
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private ImpressaoNFeService impressaoNFeService;

    @Autowired
    private AutorizacaoNFeClient autorizacaoNFeClient;

    @Autowired
    private RecepcaoEventoClient recepcaoEventoClient;

    @Autowired
    private InutilizacaoNFeClient inutilizacaoNFeClient;

    @Autowired
    private ConsultarReciboService consultarReciboService;

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Autowired
    @Qualifier("nfeAutorizacaoContext")
    private JAXBContext contextAutorizacao;

    @Autowired
    @Qualifier("nfeRetAutorizacaoContext")
    private JAXBContext contextRetAutorizacao;

    @Autowired
    @Qualifier("nfeInutilizacaoContext")
    private JAXBContext contextInutilizacao;

    @Autowired
    @Qualifier("nfeRecepcaoEventoEpecContext")
    private JAXBContext contextEpec;

    @Override
    public void fecharEnviarLote(Long idLote) {
        LOGGER.info("fecharEnviarLote {}", idLote);

        cacheLoteRepository.fecharLote(idLote);

        // Adicionando sleep para caso alguem ainda esteja adicionando ao lote
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            LOGGER.error("Erro ao executar sleep");
        }

        ResumoLote lote = cacheLoteRepository.consultarLote(idLote);
        LOGGER.info("Fechando lote {} com {} documentos ", idLote == null ? "idLote -000" : idLote, lote.getQuantidade());

        String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

        // Definicoes a partir do servico
        ServicosEnum servico;
        TipoServicoEnum tipoServico;
        String versao = lote.getVersao();

        if (ServicosEnum.AUTORIZACAO_NFE.name().equals(lote.getServico())) {
            servico = ServicosEnum.AUTORIZACAO_NFE;
            tipoServico = TipoServicoEnum.AUTORIZACAO;
        } else if (ServicosEnum.RECEPCAO_EVENTO_EPEC.name().equals(lote.getServico())) {
            servico = ServicosEnum.RECEPCAO_EVENTO_EPEC;
            tipoServico = TipoServicoEnum.AUTORIZACAO;
            versao = "4.00"; // FIXME: Utilizar versao 4.00 para servico de recepcao de eventos

        } else if (ServicosEnum.RECEPCAO_EVENTO_CANCELAMENTO.name().equals(lote.getServico())) {
            servico = ServicosEnum.RECEPCAO_EVENTO_CANCELAMENTO;
            tipoServico = TipoServicoEnum.CANCELAMENTO;
            versao = "4.00"; // FIXME: Utilizar versao 4.00 para servico de recepcao de eventos

        } else if (ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO.name().equals(lote.getServico())) {
            servico = ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO;
            tipoServico = TipoServicoEnum.MANIFESTACAO;
            versao = "4.00"; // FIXME: Utilizar versao 4.00 para servico de recepcao de eventos

        } else if (ServicosEnum.RECEPCAO_EVENTO_CARTA_CORRECAO.name().equals(lote.getServico())) {
            servico = ServicosEnum.RECEPCAO_EVENTO_CARTA_CORRECAO;
            tipoServico = TipoServicoEnum.CARTA_CORRECAO;
            versao = "4.00"; // FIXME: Utilizar versao 4.00 para servico de recepcao de eventos

        } else if (ServicosEnum.INUTILIZACAO.name().equals(lote.getServico())) {
            servico = ServicosEnum.INUTILIZACAO;
            tipoServico = TipoServicoEnum.INUTILIZACAO;

        } else {
            throw new FazemuServiceException("Servico nao encontrado na definicao do lote: " + lote.getIdLote());
        }

        // Processar documentos
        Map<DocumentoFiscal, String> map = processar(lote, usuario, servico, versao, tipoServico);

        if (map != null) {
            try {
                for (Map.Entry<DocumentoFiscal, String> entry : map.entrySet()) {
                    DocumentoFiscal docu = entry.getKey();
                    String xmlProcessado = entry.getValue();
                    kafkaProducerService.invokeCallback(docu, xmlProcessado, tipoServico);
                }
            } catch (Exception e) {
                LOGGER.error("Não foi possível produzir as mensagens de callback para o lote {} - ERRO {}", idLote, e.getMessage());

                try {
                    for (Map.Entry<DocumentoFiscal, String> entry : map.entrySet()) {
                        DocumentoFiscal docu = entry.getKey();
                        String xmlProcessado = entry.getValue();
                        kafkaProducerService.invokeCallback(docu, xmlProcessado, tipoServico);
                    }
                } catch (Exception e2) {
                    LOGGER.error("Não foi possível produzir as mensagens de callback para o lote {} - ERRO 2 {}", idLote, e2.getMessage());
                }
            }
        }
    }

    protected Map<DocumentoFiscal, String> processar(ResumoLote lote, String usuario, ServicosEnum servico, String versao, TipoServicoEnum tipoServico) {

        try {
            // obter url a partir do lote
            String url = urlRepository.getUrl(lote.getUf(), lote.getTipoEmissao(), servico.getNome(), versao);

            Document docFinal = XMLUtils.convertStringToDocument(this.montarEnvelopeXML(lote));
            docFinal.setXmlStandalone(true);

            LOGGER.info("Fechando lote {} ", lote);
            loteRepository.fecharLote(lote.getIdLote(), url, usuario);
            loteEventoRepository.insert(lote.getIdLote(), PontoLoteEnum.FECHADO.getCodigo(), usuario, null, null);

            // envia para sefaz
            if (ServicosEnum.RECEPCAO_EVENTO_EPEC.name().equals(lote.getServico())
                    || ServicosEnum.RECEPCAO_EVENTO_CANCELAMENTO.name().equals(lote.getServico())
                    || ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO.name().equals(lote.getServico())
                    || ServicosEnum.RECEPCAO_EVENTO_CARTA_CORRECAO.name().equals(lote.getServico())) {

                return enviarRecepcaoEventoParaSefaz(docFinal, url, lote, servico, tipoServico, usuario);

            } else if (ServicosEnum.AUTORIZACAO_NFE.name().equals(lote.getServico())) {

                return enviarAutorizacaoParaSefaz(docFinal, url, lote, usuario);

            } else if (ServicosEnum.INUTILIZACAO.name().equals(lote.getServico())) {

                return enviarInutilizacaoParaSefaz(docFinal, url, lote, usuario);

            }

        } catch (Exception e) {
            LOGGER.error("ERRO AO fecharEnviarLote {} devido ao erro {} ", lote.getIdLote(), e.getMessage(), e);

            // Quando acontecer um erro de integracao com a sefaz, cancela o lote
            cacheLoteRepository.cancelarLoteFechado(lote.getIdLote());
            loteRepository.cancelarLote(lote.getIdLote(), usuario);
            loteEventoRepository.insert(lote.getIdLote(), PontoLoteEnum.CANCELADO.getCodigo(), usuario, null, null);
            cacheLoteRepository.removerLoteCancelado(lote.getIdLote());

            // Logando evento de erro
            loteEventoRepository.insert(lote.getIdLote(), PontoLoteEnum.ERRO_FECHAR_LOTE.getCodigo(), usuario, null, e.getMessage());

            // Caso sejam EPEC, tira DPEC de Enviado para Aberto
            lote.getIdDocFiscalList()
                    .stream()
                    .filter((IdDocFiscal) -> (documentoEpecRepository.findByIdDocFiscalAndSituacao(IdDocFiscal, SituacaoEnum.ENVIADO) != null))
                    .forEachOrdered((IdDocFiscal) -> {
                        documentoEpecRepository.updateSituacao(IdDocFiscal, SituacaoEnum.ABERTO);
                    });
        }

        return null;
    }

    protected String montarEnvelopeXML(ResumoLote lote) {
        Long idLote = lote.getIdLote();
        LOGGER.info("montarEnvelopeXML do idLote {}", idLote);

        Map<String, Object> map = Maps.newHashMap();
        map.put("versao", lote.getVersao());
        map.put("idLote", idLote);
        if (ServicosEnum.AUTORIZACAO_NFE.name().equals(lote.getServico())) {
            map.put("indSinc", ID_SINC_ASSINCRONO);
        }

        String inicioEnvelope = "";
        String fimEnvelope = "";

        StringBuilder sb = new StringBuilder().append(XML_HEADER);

        if (ServicosEnum.AUTORIZACAO_NFE.name().equals(lote.getServico())) {
            inicioEnvelope = ENVI_INI;
            fimEnvelope = ENVI_END;

        } else if (ServicosEnum.RECEPCAO_EVENTO_EPEC.name().equals(lote.getServico())
                || ServicosEnum.RECEPCAO_EVENTO_CANCELAMENTO.name().equals(lote.getServico())
                || ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO.name().equals(lote.getServico())
                || ServicosEnum.RECEPCAO_EVENTO_CARTA_CORRECAO.name().equals(lote.getServico())) {
            inicioEnvelope = ENV_EVENTO_INI;
            fimEnvelope = ENV_EVENTO_END;
        }

        if (!ServicosEnum.INUTILIZACAO.name().equals(lote.getServico())) {
            sb.append(StrSubstitutor.replace(inicioEnvelope, map));
        }

        lote.getIdDocFiscalList()
                .stream()
                .map(idDocFiscal -> documentoFiscalRepository.getXmlByIdLoteAndIdDocFiscal(idLote, idDocFiscal))
                .forEach(xml -> {
                    sb.append(xml.startsWith(XML_HEADER) ? xml.replace(XML_HEADER, StringUtils.EMPTY).trim() : xml.trim());
                });

        if (!ServicosEnum.INUTILIZACAO.name().equals(lote.getServico())) {
            sb.append(fimEnvelope);
        }

        return sb.toString();
    }

    protected Map<DocumentoFiscal, String> enviarRecepcaoEventoParaSefaz(Document docFinal, String url, ResumoLote lote,
            ServicosEnum servico, TipoServicoEnum tipoServico, String usuario) throws Exception {
        LOGGER.info("FecharEnviarLoteServiceImpl >> enviarRecepcaoEventoParaSefaz lote {} ", lote.getIdLote());

        // Armazena o XML do Lote do Envio
        String xmlLoteEnvio = XMLUtils.convertDocumentToString(docFinal);
        Long idClobLoteEnviado = documentoClobRepository.insert(DocumentoClob.build(null, xmlLoteEnvio, usuario));
        loteEventoRepository.insert(lote.getIdLote(), PontoLoteEnum.ENVIADO.getCodigo(), usuario, idClobLoteEnviado, null);

        // Faz a integração com a SEFAZ
        br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg nfeDadosMsg = new br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg();
        nfeDadosMsg.getContent().add(docFinal.getDocumentElement());
        LOGGER.info("FecharEnviarLoteServiceImpl >> " + DateUtils.convertDateToString(new Date()) + " << LOTE {} | Enviando nferecepcaoevento ", lote);
        br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg nfeResultMsg = recepcaoEventoClient.nfeRecepcaoEvento(url, nfeDadosMsg, lote, ServicosEnum.RECEPCAO_EVENTO_EPEC);
        LOGGER.info("FecharEnviarLoteServiceImpl >> " + DateUtils.convertDateToString(new Date()) + " << LOTE {} | Retorno nferecepcaoevento ", lote);

        // recupera resultado
        List<Object> contentList = nfeResultMsg.getContent();
        Object content = contentList.get(0);
        LOGGER.debug("FecharEnviarLoteServiceImpl >> CONTENT LIST {} CONTENT {} CONTENT CLASS {}", contentList, content, content.getClass());
        TRetEnvEvento tRetEnvEvento = (TRetEnvEvento) contextEpec.createUnmarshaller().unmarshal((Node) content);

        // xml lote retorno
        Element element = (Element) content;
        String xmlLoteRetorno = XMLUtils.convertDocumentToString(element.getOwnerDocument());
        Long idClobRetorno = documentoClobRepository.insert(DocumentoClob.build(null, xmlLoteRetorno, usuario));

        Integer cStat = Integer.valueOf(tRetEnvEvento.getCStat());
        LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} CSTAT {}", lote.getIdLote(), cStat);

        String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(cStat);
        Boolean situacaoFinalizadoraLote;

        if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())
                || StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.REJEITADO_FINALIZADO.getCodigo())) {
            LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Sucesso na RecepcaoEvento | Servico {} ", lote, cStat, situacaoAutorizacao, servico.getNome());
            situacaoFinalizadoraLote = true;
        } else if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.REJEITADO_TRATAMENTO.getCodigo())) {
            LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Falha na RecepcaoEvento | Servico {} ", lote, cStat, situacaoAutorizacao, servico.getNome());
            situacaoFinalizadoraLote = false;
        } else {
            LOGGER.error("FecharEnviarLoteServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Erro não mapeado na RecepcaoEvento | Servico {} ", lote, cStat, situacaoAutorizacao, servico.getNome());
            situacaoFinalizadoraLote = false;
        }

        // processa resultado
        LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} | Finaliza RecepcaoEvento | Servico {} ", lote, servico.getNome());

        cacheLoteRepository.finalizarFechadosLote(lote.getIdLote());
        loteRepository.finalizarLote(lote.getIdLote(), cStat, usuario);
        loteEventoRepository.insert(lote.getIdLote(), PontoLoteEnum.LIQUIDADO.getCodigo(), usuario, idClobRetorno, null);

        Map<DocumentoFiscal, String> mapToCallback;
        if (situacaoFinalizadoraLote == true) {
            mapToCallback = processarDocumentosRecepcaoEvento(lote, content, tipoServico, ServicosEnum.RECEPCAO_EVENTO_EPEC.name().equals(lote.getServico()), usuario);
        } else {
            mapToCallback = reemitirLoteRecepcaoEvento(lote, content, tipoServico, usuario);
        }

        cacheLoteRepository.removerLoteFinalizado(lote.getIdLote());

        return mapToCallback;
    }

    protected Map<DocumentoFiscal, String> enviarAutorizacaoParaSefaz(Document docFinal, String url, ResumoLote lote, String usuario) throws Exception {
        LOGGER.info("FecharEnviarLoteServiceImpl >> enviarAutorizacaoParaSefaz lote {} ", lote.getIdLote());

        // Armazena o XML do Lote do Envio
        String xmlLoteEnvio = XMLUtils.convertDocumentToString(docFinal);
        Long idClobLoteEnviado = documentoClobRepository.insert(DocumentoClob.build(null, xmlLoteEnvio, usuario));
        loteEventoRepository.insert(lote.getIdLote(), PontoLoteEnum.ENVIADO.getCodigo(), usuario, idClobLoteEnviado, null);

        // Faz a integração com a SEFAZ
        br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeDadosMsg nfeDadosMsg = new br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeDadosMsg();
        nfeDadosMsg.getContent().add(docFinal.getDocumentElement());
        LOGGER.info("FecharEnviarLoteServiceImpl >> " + DateUtils.convertDateToString(new Date()) + " << LOTE {} | Enviando nfeAutorizacao ", lote);
        br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeResultMsg nfeResultMsg = autorizacaoNFeClient.nfeAutorizacaoLote(url, nfeDadosMsg, lote);
        LOGGER.info("FecharEnviarLoteServiceImpl >> " + DateUtils.convertDateToString(new Date()) + " << LOTE {} | Retorno nfeAutorizacao ", lote);

        // recupera resultado
        List<Object> contentList = nfeResultMsg.getContent();
        Object content = contentList.get(0);
        LOGGER.debug("FecharEnviarLoteServiceImpl >> CONTENT LIST {} CONTENT {} CONTENT CLASS {}", contentList, content, content.getClass());
        TRetEnviNFe tRetEnviNFe = (TRetEnviNFe) contextAutorizacao.createUnmarshaller().unmarshal((Node) content);

        // xml lote retorno
        Element element = (Element) content;
        String xmlLoteRetorno = XMLUtils.convertDocumentToString(element.getOwnerDocument());
        Long idClobRetorno = documentoClobRepository.insert(DocumentoClob.build(null, xmlLoteRetorno, usuario));

        Integer cStat = Integer.valueOf(tRetEnviNFe.getCStat());
        LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} CSTAT {}", lote.getIdLote(), cStat);

        String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(cStat);
        Boolean situacaoFinalizadora;

        if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())
                || StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.REJEITADO_FINALIZADO.getCodigo())) {
            LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Sucesso no envio da nfeAutorizacao ", lote, cStat, situacaoAutorizacao);
            situacaoFinalizadora = true;
        } else if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.REJEITADO_TRATAMENTO.getCodigo())) {
            LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Falha no envio da nfeAutorizacao ", lote, cStat, situacaoAutorizacao);
            situacaoFinalizadora = false;
        } else {
            LOGGER.error("FecharEnviarLoteServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Erro não mapeado no envio da nfeAutorizacao ", lote, cStat, situacaoAutorizacao);
            situacaoFinalizadora = false;
        }

        if (situacaoFinalizadora == true) {
            // processa resultado
            LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} | Finaliza sucesso no envio nfeAutorizacao ", lote);
            String reciboLote = tRetEnviNFe.getInfRec().getNRec();
            lote.setRecibo(reciboLote);

            loteRepository.enviarLote(lote.getIdLote(), Long.valueOf(reciboLote), cStat, usuario);
            loteEventoRepository.insert(lote.getIdLote(), PontoLoteEnum.RECEBIDO.getCodigo(), usuario, idClobRetorno, null);
            cacheLoteRepository.atualizarLote(lote);
            cacheLoteRepository.enviarLote(lote.getIdLote());

            // Timer Task Consulta Lote
            Integer tempoEspera = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_CONSULTA_LOTE, 120); //default 2 minutos === 120 segundos

            LOGGER.info("TIMER TASK DE CONSULTA DO LOTE {} - TEMPO DE ESPERA {} SEG.", lote.getIdLote(), tempoEspera);
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    consultarReciboService.consultarReciboCallback(lote);
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, (tempoEspera * 1000));

        } else {
            // processa resultado
            LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} | Finaliza falha no envio nfeAutorizacao ", lote);

            cacheLoteRepository.finalizarFechadosLote(lote.getIdLote());
            loteRepository.finalizarLote(lote.getIdLote(), cStat, usuario);
            loteEventoRepository.insert(lote.getIdLote(), PontoLoteEnum.LIQUIDADO.getCodigo(), usuario, idClobRetorno, null);
            Map<DocumentoFiscal, String> mapToCallback = desmembrarEncerrarProcessoAutorizacao(lote, tRetEnviNFe, usuario);

            cacheLoteRepository.removerLoteFinalizado(lote.getIdLote());

            return mapToCallback;
        }

        return null;
    }

    protected Map<DocumentoFiscal, String> enviarInutilizacaoParaSefaz(Document docFinal, String url, ResumoLote lote, String usuario) throws Exception {
        Long idLote = lote.getIdLote();
        LOGGER.info("FecharEnviarLoteServiceImpl >> enviarInutilizacaoParaSefaz lote {} ", lote.getIdLote());

        // Armazena o XML do Lote do Envio
        String xmlLoteEnvio = XMLUtils.convertDocumentToString(docFinal);
        Long idClobLoteEnviado = documentoClobRepository.insert(DocumentoClob.build(null, xmlLoteEnvio, usuario));
        loteEventoRepository.insert(idLote, PontoLoteEnum.ENVIADO.getCodigo(), usuario, idClobLoteEnviado, null);

        // Faz a integração com a SEFAZ
        br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeDadosMsg nfeDadosMsg = new br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeDadosMsg();
        nfeDadosMsg.getContent().add(docFinal.getDocumentElement());
        LOGGER.info("FecharEnviarLoteServiceImpl >> " + DateUtils.convertDateToString(new Date()) + " << LOTE {} | Enviando nfeinutilizacao ", lote);
        br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeResultMsg nfeResultMsg = inutilizacaoNFeClient.nfeInutilizacao(url, lote, nfeDadosMsg);
        LOGGER.info("FecharEnviarLoteServiceImpl >> " + DateUtils.convertDateToString(new Date()) + " << LOTE {} | Retorno nfeinutilizacao ", lote);

        // processa resultado
        List<Object> contentList = nfeResultMsg.getContent();
        Object content = contentList.get(0);
        LOGGER.debug("FecharEnviarLoteServiceImpl >> CONTENT LIST {} CONTENT {} CONTENT CLASS {}", contentList, content, content.getClass());
        TRetInutNFe tRetInutNFe = (TRetInutNFe) contextInutilizacao.createUnmarshaller().unmarshal((Node) content);

        // xml lote retorno
        Element element = (Element) content;
        String xmlLoteRetorno = XMLUtils.convertDocumentToString(element.getOwnerDocument());
        Long idClobRetorno = documentoClobRepository.insert(DocumentoClob.build(null, xmlLoteRetorno, usuario));

        Integer cStat = Integer.valueOf(tRetInutNFe.getInfInut().getCStat());
        LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} CSTAT {}", idLote, cStat);

        String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(cStat);

        if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())
                || StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.REJEITADO_FINALIZADO.getCodigo())) {
            LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Sucesso na consulta da Inutilizacao ", lote, cStat, situacaoAutorizacao);
        } else if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.REJEITADO_TRATAMENTO.getCodigo())) {
            LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Falha na consulta Inutilizacao ", lote, cStat, situacaoAutorizacao);
        } else {
            LOGGER.error("FecharEnviarLoteServiceImpl >> LOTE {} | CSTAT {} | SITUACAO AUTORIZACAO {} | Erro não mapeado na consulta da Inutilizacao ", lote, cStat, situacaoAutorizacao);
        }

        // processa resultado
        LOGGER.info("FecharEnviarLoteServiceImpl >> LOTE {} | Finaliza Inutilizacao ", lote);

        cacheLoteRepository.finalizarFechadosLote(idLote);
        loteRepository.finalizarLote(idLote, cStat, usuario);
        loteEventoRepository.insert(idLote, PontoLoteEnum.LIQUIDADO.getCodigo(), usuario, idClobRetorno, null);
        Map<DocumentoFiscal, String> mapToCallback = processarDocumentoInutilizacao(tRetInutNFe, usuario, lote.getIdDocFiscalList().get(0));
        cacheLoteRepository.removerLoteFinalizado(idLote);

        return mapToCallback;

    }

    protected Map<DocumentoFiscal, String> reemitirLoteRecepcaoEvento(ResumoLote lote, Object content, TipoServicoEnum tipoServico, String usuario) throws Exception {
        LOGGER.info("FecharEnviarLoteServiceImpl >> reemitirLoteRecepcaoEvento {} {} ", lote, tipoServico);

        Map<DocumentoFiscal, String> mapToCallback = Maps.newHashMap();

        // Caso lote com erro tenha mais de 1 documento, desmembra o lote para reenvio
        if (lote.getIdDocFiscalList().size() > 1) {
            desmembrarLote(lote, true);
        } // Caso seja apenas um documento, liquida o documento
        else {

            if (TipoServicoEnum.CARTA_CORRECAO.equals(tipoServico)) {
                return processarRetornoCartaCorrecaoService.liquidarCartaCorrecao(lote, content, tipoServico, usuario);

            } else {
                //Preenche dado que a SEFAZ não devolve em caso de falha
                DocumentoFiscal docu = documentoFiscalRepository.findById(lote.getIdDocFiscalList().get(0));

                TRetEnvEvento tRetEnvEvento = (TRetEnvEvento) contextEpec.createUnmarshaller().unmarshal((Node) content);

                //seta a chave no retorno para identificação no sistema cliente
                if (tRetEnvEvento.getRetEvento() == null || tRetEnvEvento.getRetEvento().isEmpty()) {
                    TRetEvento.InfEvento infEvento = new TRetEvento.InfEvento();
                    infEvento.setChNFe(docu.getChaveAcesso());

                    TRetEvento tRetEvento = new TRetEvento();
                    tRetEvento.setInfEvento(infEvento);

                    tRetEnvEvento.getRetEvento().add(tRetEvento);
                } else {
                    tRetEnvEvento.getRetEvento().get(0).getInfEvento().setChNFe(docu.getChaveAcesso());
                }
                LOGGER.info("Setou chaveAcesso no tRetEnvEvento {} ", docu.getChaveAcesso());

                Document xml = XMLUtils.createNewDocument();
                contextEpec.createMarshaller().marshal(tRetEnvEvento, xml);
                LOGGER.info("Parse via Jaxb");

                String xmlProcessado = XMLUtils.convertDocumentToString(xml);
                LOGGER.info("xmlProcessado {}", xmlProcessado);

                Long idClob = documentoClobRepository.insert(DocumentoClob.build(null, xmlProcessado, usuario));
                LOGGER.info("Criou clob processado {}", idClob);

                documentoEventoRepository.insert(DocumentoEvento.build(null, lote.getIdDocFiscalList().get(0), PontoDocumentoEnum.PROCESSADO.getCodigo(), tipoServico.getTipoRetorno(), tRetEnvEvento.getCStat(), idClob, usuario));
                LOGGER.info("Criou evento do documento");

                String situacaoDocumento = documentoFiscalRepository.getSituacaoDocumentoByIdDocFiscal(lote.getIdDocFiscalList().get(0));
                if (SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)) {
                    situacaoDocumento = SituacaoDocumentoEnum.REJEITADO.getCodigo();
                } else if (situacaoDocumento.isEmpty()) {
                    situacaoDocumento = SituacaoDocumentoEnum.REJEITADO.getCodigo();
                }

                documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(lote.getIdDocFiscalList().get(0), tRetEnvEvento.getCStat(), PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, null, situacaoDocumento);

                //Grava a documento retorno apenas quando documento foi processado com sucesso
                String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(Integer.valueOf(tRetEnvEvento.getCStat()));
                if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())) {
                    Long tipoEvento = null;
                    if (TipoServicoEnum.MANIFESTACAO.getTipoRetorno().equals(tipoServico.getTipoRetorno())) {
                        tipoEvento = Long.valueOf(tRetEnvEvento.getRetEvento().get(0).getInfEvento().getTpEvento());
                        LOGGER.info("FecharEnviarLoteServiceImpl - reemitirLoteRecepcaoEvento >> tRetEnvEvento.getRetEvento().get(0).getInfEvento().getTpEvento() {} ", tRetEnvEvento.getRetEvento().get(0).getInfEvento().getTpEvento());
                    }

                    if (documentoRetornoRepository.findByIdDocFiscalAndTpServicoAndTpEvento(lote.getIdDocFiscalList().get(0), tipoServico.getTipoRetorno(), tipoEvento) == null) {
                        documentoRetornoRepository.insert(lote.getIdDocFiscalList().get(0), tipoServico.getTipoRetorno(), tipoEvento, idClob, usuario, usuario);
                    } else {
                        documentoRetornoRepository.update(lote.getIdDocFiscalList().get(0), tipoServico.getTipoRetorno(), tipoEvento, idClob, usuario);
                    }
                }

                // Adiciona ao map do callback
                mapToCallback.put(docu, xmlProcessado);
            }
        }

        return mapToCallback;
    }

    /**
     * Usado para todas as operacoes
     *
     * @param lote
     * @param isReprocessamento
     */
    protected void desmembrarLote(ResumoLote lote, boolean isReprocessamento) {
        LOGGER.info("FecharEnviarLoteServiceImpl >> desmembrarLote {} ", lote.getIdLote());

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

    protected Map<DocumentoFiscal, String> processarDocumentosRecepcaoEvento(ResumoLote lote, Object content, TipoServicoEnum tipoServico, Boolean isEpec, String usuario) throws Exception {
        LOGGER.info("FecharEnviarLoteServiceImpl >> processarDocumentosRecepcaoEvento {} ", lote.getIdLote());

        Map<DocumentoFiscal, String> mapToCallback = Maps.newHashMap();

        if (TipoServicoEnum.CARTA_CORRECAO.equals(tipoServico)) {

            return processarRetornoCartaCorrecaoService.processarDocumentosCartaCorrecao(lote.getIdLote(), content, tipoServico, usuario);

        } else {
            TRetEnvEvento tRetEnvEvento = (TRetEnvEvento) contextEpec.createUnmarshaller().unmarshal((Node) content);

            for (TRetEvento tRetEvento : ListUtils.emptyIfNull(tRetEnvEvento.getRetEvento())) {

                String chaveAcesso = tRetEvento.getInfEvento().getChNFe();
                DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);
                //Caso não encontre, busca pelos dados da nota fiscal
                if (documentoFiscal == null) {
                    ChaveAcessoNFe key = ChaveAcessoNFe.unparseKey(chaveAcesso);
                    Integer anoKey = FazemuUtils.getFullYearFormat(Integer.valueOf(key.dataAAMM.substring(0, 2)));
                    Long idEstado = estadoRepository.findByCodigoIbge(Integer.valueOf(key.cUF)).getId();
                    documentoFiscal = documentoFiscalRepository.findByDadosDocumentoFiscal(TIPO_DOCUMENTO_FISCAL_NFE, Long.valueOf(key.cnpjCpf), Long.valueOf(key.nNF), Long.valueOf(key.serie), anoKey, idEstado);
                }
                Long idDocFiscal = documentoFiscal.getId();

                String xmlProcessado = gerarXmlProcessadoRecepcaoEvento(tRetEvento, idDocFiscal, tipoServico, isEpec, usuario);

                Long idClob = documentoClobRepository.insert(DocumentoClob.build(null, xmlProcessado, usuario));

                documentoEventoRepository.insert(DocumentoEvento.build(null, idDocFiscal, PontoDocumentoEnum.PROCESSADO.getCodigo(), tipoServico.getTipoRetorno(), tRetEvento.getInfEvento().getCStat(), idClob, usuario));

                String situacaoDocumento = documentoFiscal.getSituacaoDocumento();
                String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(Integer.valueOf(tRetEvento.getInfEvento().getCStat()));

                if ((SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                        || SituacaoDocumentoEnum.REJEITADO.getCodigo().equals(situacaoDocumento))
                        && CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
                    if (TipoServicoEnum.AUTORIZACAO.getTipoRetorno().equals(tipoServico.getTipoRetorno())) {
                        situacaoDocumento = SituacaoDocumentoEnum.AUTORIZADO.getCodigo();
                    } else if (TipoServicoEnum.CANCELAMENTO.getTipoRetorno().equals(tipoServico.getTipoRetorno())) {
                        situacaoDocumento = SituacaoDocumentoEnum.CANCELADO.getCodigo();
                    }
                } else if (SituacaoDocumentoEnum.AUTORIZADO.getCodigo().equals(situacaoDocumento)
                        && CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
                    if (TipoServicoEnum.CANCELAMENTO.getTipoRetorno().equals(tipoServico.getTipoRetorno())) {
                        situacaoDocumento = SituacaoDocumentoEnum.CANCELADO.getCodigo();
                    }
                } else if (SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                        && !CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
                    situacaoDocumento = SituacaoDocumentoEnum.REJEITADO.getCodigo();
                }

                Long tipoEmissao = documentoFiscal.getTipoEmissao();
                if (tipoEmissao == null || tipoEmissao == 0) {
                    tipoEmissao = lote.getTipoEmissao().longValue();
                }

                documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(idDocFiscal, tRetEvento.getInfEvento().getCStat(), PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, tipoEmissao, situacaoDocumento);

                //Grava a documento retorno apenas quando documento foi processado com sucesso
                if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())) {
                    Long tipoEvento = null;
                    if (TipoServicoEnum.MANIFESTACAO.getTipoRetorno().equals(tipoServico.getTipoRetorno())) {
                        tipoEvento = Long.valueOf(tRetEvento.getInfEvento().getTpEvento());
                    }

                    if (documentoRetornoRepository.findByIdDocFiscalAndTpServicoAndTpEvento(idDocFiscal, tipoServico.getTipoRetorno(), tipoEvento) == null) {
                        documentoRetornoRepository.insert(idDocFiscal, tipoServico.getTipoRetorno(), tipoEvento, idClob, usuario, usuario);
                    } else {
                        documentoRetornoRepository.update(idDocFiscal, tipoServico.getTipoRetorno(), tipoEvento, idClob, usuario);
                    }
                }

                if (isEpec) {
                    if ("136".equals(tRetEvento.getInfEvento().getCStat())) {
                        try {
                            // Cria documento_epec para envio ao autorizador normal
                            documentoEpecRepository.insert(DocumentoEpec.build(documentoFiscal.getId(), estadoRepository.findByCodigoIbge(lote.getUf()).getId(), TIPO_DOCUMENTO_FISCAL_NFE, SituacaoEnum.ABERTO.getCodigo(), null, usuario));
                        } catch (Exception e) {
                            LOGGER.error("Não conseguiu inserir documento epec para documento {} ", documentoFiscal.getId());
                            documentoEpecRepository.insert(DocumentoEpec.build(documentoFiscal.getId(), estadoRepository.findByCodigoIbge(lote.getUf()).getId(), TIPO_DOCUMENTO_FISCAL_NFE, SituacaoEnum.ABERTO.getCodigo(), null, usuario));
                        }

                        try {
                            // Faz impressao em segunda via (escritorio)
                            impressaoNFeService.segundaViaImpressao(documentoFiscal, lote.getTipoEmissao());
                        } catch (Exception e) {
                            LOGGER.error("Não conseguiu imprimir segunda via do documento {} no Fiscal ", documentoFiscal.getId());
                        }
                    } else if ("468".equals(tRetEvento.getInfEvento().getCStat())) {
                        try {
                            DocumentoEpec documentoEpec = documentoEpecRepository.findByIdDocumentoFiscalAndIdEstado(documentoFiscal.getId(), documentoFiscal.getIdEstado());

                            //Validar se realmente foi emitida antes
                            if (documentoEpec != null) {
                                documentoEpecRepository.updateSituacao(documentoFiscal.getId(), SituacaoEnum.LIQUIDADO);
                            } else {
                                assinarDocumentoTipoEmissaoNormal(documentoFiscal, true);

                                xmlProcessado = gerarXmlProcessadoRecepcaoEvento(tRetEvento, idDocFiscal, tipoServico, isEpec, usuario);
                            }
                        } catch (Exception e) {
                            assinarDocumentoTipoEmissaoNormal(documentoFiscal, true);

                            xmlProcessado = gerarXmlProcessadoRecepcaoEvento(tRetEvento, idDocFiscal, tipoServico, isEpec, usuario);
                        }
                    } else {
                        if ("142".equals(tRetEvento.getInfEvento().getCStat())) {
                            //Rejeição por EPEC bloqueado
                            assinarDocumentoTipoEmissaoNormal(documentoFiscal, true);
                        } else {
                            //Rejeição por outros motivos
                            assinarDocumentoTipoEmissaoNormal(documentoFiscal, false);
                        }

                        xmlProcessado = gerarXmlProcessadoRecepcaoEvento(tRetEvento, idDocFiscal, tipoServico, isEpec, usuario);
                    }
                }

                // Adiciona ao map do callback
                mapToCallback.put(documentoFiscal, xmlProcessado);
            }
        }

        LOGGER.info("Atualizado retorno do lote {}", lote.getIdLote());
        return mapToCallback;
    }

    protected String gerarXmlProcessadoRecepcaoEvento(TRetEvento tRetEvento, Long idDocFiscal, TipoServicoEnum tipoServico, Boolean isEpec, String usuario) throws JAXBException {

        StringResult stringResult = new StringResult();
        // Encaixa protocolo para epec
        if (isEpec) {
            String xmlNFe = documentoClobRepository.getLastXmlSignedByIdDocFiscal(idDocFiscal).getClob();

            TProtNFe protNFe = new TProtNFe();

            TProtNFe.InfProt infProt = new TProtNFe.InfProt();
            infProt.setCStat(tRetEvento.getInfEvento().getCStat());
            infProt.setVerAplic(tRetEvento.getInfEvento().getVerAplic());
            infProt.setChNFe(tRetEvento.getInfEvento().getChNFe());
            infProt.setTpAmb(tRetEvento.getInfEvento().getTpAmb());
            infProt.setDhRecbto(tRetEvento.getInfEvento().getDhRegEvento());
            infProt.setXMotivo(tRetEvento.getInfEvento().getXMotivo());
            infProt.setNProt(tRetEvento.getInfEvento().getNProt());
            protNFe.setInfProt(infProt);

            TNFe tNFe = (TNFe) contextRetAutorizacao.createUnmarshaller().unmarshal(new StringSource(xmlNFe));

            TNfeProc nfeProc = new TNfeProc();
            nfeProc.setNFe(tNFe);
            nfeProc.setProtNFe(protNFe);
            nfeProc.setVersao(protNFe.getVersao());

            contextRetAutorizacao.createMarshaller().marshal(nfeProc, stringResult);

        } else {
            contextEpec.createMarshaller().marshal(tRetEvento, stringResult);
        }

        return stringResult.toString();
    }

    protected Map<DocumentoFiscal, String> processarDocumentoInutilizacao(TRetInutNFe tRetInutNFe, String usuario, Long idDocFiscal) throws Exception {
        Map<DocumentoFiscal, String> mapToCallback = Maps.newHashMap();

        try {
            DocumentoFiscal docu = documentoFiscalRepository.findById(idDocFiscal);

            //Preenche dado que a SEFAZ não devolve em caso de falha
            tRetInutNFe.getInfInut().setCNPJ(String.valueOf(docu.getIdEmissor()));
            tRetInutNFe.getInfInut().setNNFIni(String.valueOf(docu.getNumeroDocumentoFiscal()));
            tRetInutNFe.getInfInut().setNNFFin(String.valueOf(docu.getNumeroDocumentoFiscal()));
            tRetInutNFe.getInfInut().setSerie(String.valueOf(docu.getSerieDocumentoFiscal()));

            StringResult stringResult = new StringResult();
            contextInutilizacao.createMarshaller().marshal(tRetInutNFe, stringResult);
            String xmlProcessado = stringResult.toString();

            Long idClob = documentoClobRepository.insert(DocumentoClob.build(null, xmlProcessado, usuario));

            documentoEventoRepository.insert(DocumentoEvento.build(null, idDocFiscal, PontoDocumentoEnum.PROCESSADO.getCodigo(), TipoServicoEnum.INUTILIZACAO.getTipoRetorno(), tRetInutNFe.getInfInut().getCStat(), idClob, usuario));

            String situacaoDocumento = documentoFiscalRepository.getSituacaoDocumentoByIdDocFiscal(idDocFiscal);
            String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(Integer.valueOf(tRetInutNFe.getInfInut().getCStat()));

            if (CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
                situacaoDocumento = SituacaoDocumentoEnum.INUTILIZADO.getCodigo();
            } else if (SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                    && !CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
                situacaoDocumento = SituacaoDocumentoEnum.REJEITADO.getCodigo();
            }

            documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(idDocFiscal, tRetInutNFe.getInfInut().getCStat(), PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, null, situacaoDocumento);

            //Grava a documento retorno apenas quando documento foi processado com sucesso
            if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())) {
                if (documentoRetornoRepository.findByIdDocFiscalAndTpServicoAndTpEvento(idDocFiscal, TipoServicoEnum.INUTILIZACAO.getTipoRetorno(), null) == null) {
                    documentoRetornoRepository.insert(idDocFiscal, TipoServicoEnum.INUTILIZACAO.getTipoRetorno(), null, idClob, usuario, usuario);
                } else {
                    documentoRetornoRepository.update(idDocFiscal, TipoServicoEnum.INUTILIZACAO.getTipoRetorno(), null, idClob, usuario);
                }
            }

            // Adiciona ao map do callback
            mapToCallback.put(docu, xmlProcessado);

        } catch (Exception e) {
            LOGGER.error("Erro ao processar documento inutilizacao {}", idDocFiscal, e);
            throw new Exception();
        }

        return mapToCallback;

    }

    protected Map<DocumentoFiscal, String> desmembrarEncerrarProcessoAutorizacao(ResumoLote lote, TRetEnviNFe tRetEnviNFe, String usuario) throws JsonProcessingException, Exception {
        LOGGER.info("desmembrarEncerrarProcesso ResumoLote {}, TRetConsReciNFe {} ", lote, tRetEnviNFe);

        Map<DocumentoFiscal, String> mapToCallback = null;

        List<Long> idDocFiscalList = lote.getIdDocFiscalList();
        if (idDocFiscalList.size() > 1) {
            desmembrarLote(lote, true);

        } else if (tRetEnviNFe != null) {
            Long idDocFiscal = idDocFiscalList.get(0);
            DocumentoFiscal docu = documentoFiscalRepository.findById(idDocFiscal);
            String xmlNFe = documentoFiscalRepository.getXmlByIdLoteAndIdDocFiscal(lote.getIdLote(), idDocFiscal);

            //constroi TProtNFe artificial com dados do retorno do lote e nao da NFe
            com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TProtNFe.InfProt infProt = new com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TProtNFe.InfProt();
            infProt.setTpAmb(tRetEnviNFe.getTpAmb());
            infProt.setVerAplic(tRetEnviNFe.getVerAplic());
            infProt.setChNFe(docu.getChaveAcesso());
            infProt.setDhRecbto(tRetEnviNFe.getDhRecbto());
            infProt.setCStat(tRetEnviNFe.getCStat()); //nesse cenario NFe ficara com cStat e xMotivo do recibo do lote e nao com o cStat da propria NFe na SeFaz como de praxe
            infProt.setXMotivo(tRetEnviNFe.getXMotivo());
            com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TProtNFe protNFe = new com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TProtNFe();
            protNFe.setInfProt(infProt);

            String xmlProcessado = this.encaixarProtocoloFalhaAutorizacao(xmlNFe, protNFe, idDocFiscal, usuario);

            // Adiciona ao map do callback
            mapToCallback = Maps.newHashMap();
            mapToCallback.put(docu, xmlProcessado);
        }
        return mapToCallback;
    }

    public String encaixarProtocoloFalhaAutorizacao(String xmlNFe, com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TProtNFe protNFe, Long idDocFiscal, String usuario) throws JAXBException {
        LOGGER.info("encaixarProtocoloFalhaAutorizacao {} ", idDocFiscal);
        com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TNFe tNFe = (com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TNFe) contextAutorizacao.createUnmarshaller().unmarshal(new StringSource(xmlNFe));

        com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TNfeProc nfeProc = new com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TNfeProc();
        nfeProc.setNFe(tNFe);
        nfeProc.setProtNFe(protNFe);
        nfeProc.setVersao(protNFe.getVersao());

        StringResult stringResult = new StringResult();
        contextAutorizacao.createMarshaller().marshal(nfeProc, stringResult);
        String result = stringResult.toString();

        Long idClob = documentoClobRepository.insert(DocumentoClob.build(null, result, usuario));
        documentoEventoRepository.insert(DocumentoEvento.build(null, idDocFiscal, PontoDocumentoEnum.PROCESSADO.getCodigo(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), protNFe.getInfProt().getCStat(), idClob, usuario));

        String situacaoDocumento = documentoFiscalRepository.getSituacaoDocumentoByIdDocFiscal(idDocFiscal);
        String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(Integer.valueOf(protNFe.getInfProt().getCStat()));

        if ((SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                || SituacaoDocumentoEnum.REJEITADO.getCodigo().equals(situacaoDocumento))
                && CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
            situacaoDocumento = SituacaoDocumentoEnum.AUTORIZADO.getCodigo();
        } else if (SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                && !CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
            situacaoDocumento = SituacaoDocumentoEnum.REJEITADO.getCodigo();
        }

        documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(idDocFiscal, protNFe.getInfProt().getCStat(), PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, null, situacaoDocumento);

        //Grava a documento retorno apenas quando documento foi processado com sucesso
        if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())) {
            if (documentoRetornoRepository.findByIdDocFiscalAndTpServicoAndTpEvento(idDocFiscal, TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null) == null) {
                documentoRetornoRepository.insert(idDocFiscal, TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null, idClob, usuario, usuario);
            } else {
                documentoRetornoRepository.update(idDocFiscal, TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null, idClob, usuario);
            }
        }

        return result;
    }

    protected void assinarDocumentoTipoEmissaoNormal(DocumentoFiscal documentoFiscal, Boolean isReenviar) throws AssinaturaDigitalException, TransformerException, Exception {
        LOGGER.info("FecharEnviarLoteServiceImpl >> assinarDocumentoTipoEmissaoNormal {} ", documentoFiscal.getId());

        String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

        DocumentoClob docClob = documentoClobRepository.getLastXmlByIdDocFiscalAndPontoDocumento(documentoFiscal.getId(), PontoDocumentoEnum.RECEBIDO_XML_RAW);

        // Certificado
        long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(Long.valueOf(documentoFiscal.getIdEmissor()));
        CertificadoDigital certificado = certificadoDigitalService.getByIdEmissorRaiz(raizCnpjEmitente);

        Document docFinal = XMLUtils.convertStringToDocument(docClob.getClob());

        FazemuUtils.signXml(docFinal, certificado, ServicosEnum.AUTORIZACAO_NFE);

        //Quando rejeição, volta para tipo de emissão normal
        Long tipoEmissao = Long.valueOf(parametrosInfraRepository.getAsInteger(null, ParametrosInfraRepository.PAIN_TP_EMISSAO));

        Long idDocumentoXmlSignedClob = documentoClobRepository.insert(DocumentoClob.build(null, XMLUtils.convertDocumentToString(docFinal), usuario));
        documentoEventoRepository.insert(DocumentoEvento.build(null, documentoFiscal.getId(), PontoDocumentoEnum.RECEBIDO_XML_SIGNED.getCodigo(), null, null, idDocumentoXmlSignedClob, usuario));

        if (isReenviar) {
            documentoFiscalRepository.updatePontoAndChaveAcessoEnviadaAndTipoEmissaoAndSituacao(documentoFiscal.getId(), PontoDocumentoEnum.RECEBIDO_XML_SIGNED, documentoFiscal.getChaveAcesso(), tipoEmissao, SituacaoEnum.ABERTO);
        } else {
            documentoFiscalRepository.updatePontoAndChaveAcessoEnviadaAndTipoEmissaoAndSituacao(documentoFiscal.getId(), PontoDocumentoEnum.PROCESSADO, documentoFiscal.getChaveAcesso(), tipoEmissao, SituacaoEnum.LIQUIDADO);
        }
    }

    @Override
    public void reconstruirLotesFechados() {
        LOGGER.info("reconstruirLotesFechados");
        List<Lote> loteList = loteRepository.obterLotesPorSituacao(SituacaoLoteEnum.FECHADO.getCodigo());
        if (CollectionUtils.isNotEmpty(loteList)) {
            long currentTimeMillis = System.currentTimeMillis();
            String key = "lotesFechados" + currentTimeMillis;
            loteList.forEach((lote) -> {
                Long idLote = lote.getId();
                LOGGER.debug("reconstruirLotesFechados lote {}", idLote);
                ResumoLote resumoLote = ResumoLote.fromLote(lote, documentoLoteRepository.listByIdDocFiscal(idLote));
                resumoLote.setUf(estadoRepository.findById(lote.getIdEstado()).getCodigoIbge());
                resumoLote.setDataAbertura(new Date());

                cacheLoteRepository.abrirLote(resumoLote, key); //eh a mesma funcao de abertura mesmo, porque nao foi enviado
                loteRepository.reabrirLote(idLote, parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT));
            });
            cacheLoteRepository.sobreporLotesAbertos(key);
        }
    }
}
