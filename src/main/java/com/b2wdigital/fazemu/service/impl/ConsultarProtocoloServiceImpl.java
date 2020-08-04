package com.b2wdigital.fazemu.service.impl;

import java.util.Calendar;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.el.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.b2wdigital.fazemu.business.client.ConsultarProtocoloClient;
import com.b2wdigital.fazemu.business.repository.CodigoRetornoAutorizadorRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoEventoRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoRetornoRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.ConsultarProtocoloService;
import com.b2wdigital.fazemu.business.service.KafkaProducerService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.enumeration.CodigoRetornoAutorizadorEnum;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;
import com.b2wdigital.fazemu.enumeration.TipoEmissaoEnum;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TConsSitNFe;
import com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TProtNFe;
import com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TRetConsSitNFe;

import br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeResultMsg;
import com.b2wdigital.assinatura_digital.CertificadoDigital;
import com.b2wdigital.fazemu.business.repository.DocumentoEpecRepository;
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.business.service.EmitirLoteService;
import com.b2wdigital.fazemu.domain.DocumentoEpec;
import com.b2wdigital.fazemu.domain.ResumoDocumentoFiscal;

/**
 * Consultar Protocolo Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class ConsultarProtocoloServiceImpl extends AbstractNFeServiceImpl implements ConsultarProtocoloService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarProtocoloServiceImpl.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private ConsultarProtocoloClient consultarProtocoloClient;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoEpecRepository documentoEpecRepository;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private DocumentoRetornoRepository documentoRetornoRepository;

    @Autowired
    private DocumentoEventoRepository documentoEventoRepository;

    @Autowired
    private CodigoRetornoAutorizadorRepository codigoRetornoAutorizadorRepository;

    @Autowired
    private EmitirLoteService emitirLoteService;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Autowired
    @Qualifier("nfeRetAutorizacaoContext")
    private JAXBContext contextRetAutorizacao;

    @Autowired
    @Qualifier("nfeConsultarProtocoloContext")
    private JAXBContext contextConsultaProtocolo;

    /**
     * process
     *
     * @param request
     * @return
     * @throws Exception
     */
    @Override
    public NfeResultMsg process(NfeDadosMsg request) throws Exception {
        try {
            LOGGER.debug("ConsultarProtocoloServiceImpl: process");

            // Obtem documento pela raiz
            Document docConsSitNFe = getDocument(request);

            // retira namespace
            XMLUtils.cleanNameSpace(docConsSitNFe);

            // Converte para String e retira espacos e quebras de linha
            String xmlFinal = XMLUtils.convertDocumentToString(docConsSitNFe);

            // Converte para Document
            Document docFinal = XMLUtils.convertStringToDocument(xmlFinal);
            docFinal.setXmlStandalone(true);

            TConsSitNFe tConsSitNFe = (TConsSitNFe) JAXBIntrospector.getValue(contextConsultaProtocolo.createUnmarshaller().unmarshal(docConsSitNFe));
            preValidations(tConsSitNFe);

            ChaveAcessoNFe chaveAcesso = ChaveAcessoNFe.unparseKey(tConsSitNFe.getChNFe());

            //Determina o tipo de emissao
            Integer tipoEmissao = getTipoEmissao(chaveAcesso.cUF);

            NfeDadosMsg nfeDadosMsg = new NfeDadosMsg();
            nfeDadosMsg.getContent().add(docFinal.getDocumentElement());

            String url = getUrl(Integer.valueOf(chaveAcesso.cUF), tipoEmissao, ServicosEnum.CONSULTA_PROTOCOLO.getNome(), tConsSitNFe.getVersao());

            return consultarProtocoloClient.nfeConsultarProtocolo(url, chaveAcesso, tConsSitNFe.getVersao(), nfeDadosMsg);

        } catch (FazemuServiceException e) {
            throw new Exception(e.getMessage());
        }
    }

    protected Document getDocument(NfeDadosMsg nfeDadosMsg) throws JAXBException, ParserConfigurationException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        contextConsultaProtocolo.createMarshaller().marshal(nfeDadosMsg, document);

        //delete nfeDadosMsg
        Node node = document.getFirstChild().getChildNodes().item(0);
        factory.setNamespaceAware(true);
        Document newDocument = builder.newDocument();
        Node importedNode = newDocument.importNode(node, true);
        newDocument.appendChild(importedNode);

        return newDocument;
    }

    protected void preValidations(TConsSitNFe tConsSitNFe) {

        String chaveAcesso = tConsSitNFe.getChNFe();

        // Verifica informacoes obrigatorias
        if (StringUtils.isBlank(chaveAcesso)) {
            throw new FazemuServiceException("Chave de Acesso não encontrada no XML");
        }

        // Valida chave acesso
        ChaveAcessoNFe key;
        try {
            key = ChaveAcessoNFe.unparseKey(chaveAcesso);

        } catch (org.apache.el.parser.ParseException e) {
            throw new FazemuServiceException("Chave de Acesso invalida");
        }

        // Verifica se UF Emitente esta cadastrado
        Estado estado = findEstadoByCodigoIbge(Integer.valueOf(key.cUF));
        if (estado == null) {
            throw new FazemuServiceException("Estado do Emitente não cadastrado: " + key.cUF);
        }
    }

    @Override
    public TRetConsSitNFe consultarProtocolo(String chaveAcesso) throws Exception {
        TConsSitNFe tConsSitNFe = new TConsSitNFe();
        tConsSitNFe.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
        tConsSitNFe.setXServ("CONSULTAR");
        tConsSitNFe.setChNFe(chaveAcesso);
        tConsSitNFe.setVersao(ServicosEnum.CONSULTA_PROTOCOLO.getVersao());

        Document document = XMLUtils.createNewDocument();
        contextConsultaProtocolo.createMarshaller().marshal(tConsSitNFe, document);
        br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeDadosMsg nfeDadosMsg = new br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeDadosMsg();
        nfeDadosMsg.getContent().add(document.getDocumentElement());

        br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeResultMsg nfeResultMsg = this.process(nfeDadosMsg);

        List<Object> contentList = nfeResultMsg.getContent();
        Object content = contentList.get(0);
        return (TRetConsSitNFe) contextConsultaProtocolo.createUnmarshaller().unmarshal((Node) content);
    }

    @Override
    public String atualizarConsultarProtocolo(String chaveAcesso, String tipoServico, Boolean isGerarNota) throws Exception {

        DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);

        if (documentoFiscal != null) {

            //Valida se a emissão é de EPEC
            if (Long.valueOf(TipoEmissaoEnum.EPEC.getCodigo()).equals(documentoFiscal.getTipoEmissao())) {
                try {
                    DocumentoEpec documentoEpec = documentoEpecRepository.findByIdDocumentoFiscalAndIdEstado(documentoFiscal.getId(), documentoFiscal.getIdEstado());

                    //Validar se realmente foi emitida antes
                    if (documentoEpec == null) {
                        assinarDocumento(documentoFiscal);
                    }
                } catch (Exception e) {
                    assinarDocumento(documentoFiscal);
                }
            }

            TRetConsSitNFe tRetConsSitNFe = null;
            try {
                tRetConsSitNFe = consultarProtocolo(documentoFiscal.getChaveAcessoEnviada());
                LOGGER.info("atualizarConsultarProtocolo - consultarProtocolo {}", documentoFiscal);
            } catch (Exception e) {
                LOGGER.error("Erro ao consultar o protocolo para chave {} ", documentoFiscal.getChaveAcessoEnviada(), e);

                return "atualizarConsultarProtocolo - Erro ao processar consulta protocolo";
            }

            if (tRetConsSitNFe != null) {

                if ("217".equals(tRetConsSitNFe.getCStat())) {

                    if (Boolean.TRUE.equals(isGerarNota)) {
                        try {
                            LOGGER.info("ConsultarProtocoloServiceImpl - NF-e não localizada na SEFAZ {}", documentoFiscal.getId());

                            String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

                            DocumentoClob docClob = documentoClobRepository.getLastXmlByIdDocFiscalAndPontoDocumento(documentoFiscal.getId(), PontoDocumentoEnum.RECEBIDO_XML_RAW);

                            // Certificado
                            long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(Long.valueOf(documentoFiscal.getIdEmissor()));
                            CertificadoDigital certificado = certificadoDigitalService.getByIdEmissorRaiz(raizCnpjEmitente);

                            Document docFinal = XMLUtils.convertStringToDocument(docClob.getClob());

                            FazemuUtils.signXml(docFinal, certificado, ServicosEnum.AUTORIZACAO_NFE);

                            Long idDocumentoXmlSignedClob = documentoClobRepository.insert(DocumentoClob.build(null, XMLUtils.convertDocumentToString(docFinal), usuario));
                            documentoEventoRepository.insert(DocumentoEvento.build(null, documentoFiscal.getId(), PontoDocumentoEnum.RECEBIDO_XML_SIGNED.getCodigo(), null, null, idDocumentoXmlSignedClob, usuario));

                            docClob.setId(idDocumentoXmlSignedClob);
                            docClob.setClob(XMLUtils.convertDocumentToString(docFinal));
                            Integer tipoEmissao = 1;

                            emitirNovoLote(tRetConsSitNFe, documentoFiscal, tipoEmissao, ServicosEnum.AUTORIZACAO_NFE, docClob);

                            return "atualizarConsultarProtocolo - 217 - novoLote";
                        } catch (Exception e) {
                            LOGGER.error("Erro ao processar consulta protocolo para chave {} ", documentoFiscal.getChaveAcessoEnviada(), e);

                            return "atualizarConsultarProtocolo - Erro ao processar consulta protocolo";
                        }
                    } else {
                        return "atualizarConsultarProtocolo - 217 - Nota não emitida";
                    }

                } else {

                    try {
                        return processarConsultaProtocolo(tRetConsSitNFe, documentoFiscal, tipoServico);
                    } catch (Exception e) {
                        LOGGER.error("Erro ao processar consulta protocolo para chave {} ", documentoFiscal.getChaveAcessoEnviada(), e);

                        return "atualizarConsultarProtocolo - Erro ao processar consulta protocolo";
                    }
                }
            } else {
                return "atualizarConsultarProtocolo - Retorno não identificado";
            }
        }

        return "atualizarConsultarProtocolo - DocumentoFiscal não localizado";
    }

    private String processarConsultaProtocolo(TRetConsSitNFe tRetConsSitNFe, DocumentoFiscal documentoFiscal, String tipoServico) throws Exception {
        LOGGER.info("processarConsultaProtocolo {} CSTAT {} ", documentoFiscal.getId(), tRetConsSitNFe.getCStat());

        String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

        TipoServicoEnum tipoServicoEnum = TipoServicoEnum.getByTipoRetorno(tipoServico);

        //TODO fazer para outros tipoServico
        if (tipoServicoEnum.equals(TipoServicoEnum.AUTORIZACAO)) {

            DocumentoClob documentoClob = documentoClobRepository.getLastXmlSignedByIdDocFiscal(documentoFiscal.getId());

            if (documentoClob == null) {
                documentoClob = assinarDocumento(documentoFiscal);
            }

            String xmlProcessado = this.encaixarProtocolo(documentoClob.getClob(), tRetConsSitNFe, documentoFiscal, usuario);

            if (xmlProcessado != null
                    && !"217".equals(xmlProcessado)
                    && !"656".equals(xmlProcessado)) {
                kafkaProducerService.invokeCallback(documentoFiscal, xmlProcessado, TipoServicoEnum.AUTORIZACAO);

                return "processarConsultaProtocolo - xmlProcessadoAutorizacao";
            }

            return xmlProcessado;
        } else if (tipoServicoEnum.equals(TipoServicoEnum.CANCELAMENTO)) {
            if ("101".equals(tRetConsSitNFe.getCStat()) || tRetConsSitNFe.getRetCancNFe() != null) {
                LOGGER.info("emitirLoteDocumentosAbertosParados (scheduled) - Processar Retorno de Cancelamento {}", documentoFiscal.getId());
                processarDocumentosRecepcaoEvento(tRetConsSitNFe, documentoFiscal, TipoServicoEnum.CANCELAMENTO);

                return "processarConsultaProtocolo - xmlProcessadoCancelamento";
            }
            return "processarConsultaProtocolo - Retorno de Cancelamento não disponível";
        } else if (tipoServicoEnum.equals(TipoServicoEnum.CARTA_CORRECAO)) {
            if (tRetConsSitNFe.getProcEventoNFe() != null) {
                LOGGER.info("emitirLoteDocumentosAbertosParados (scheduled) - Processar Retorno de Carta de Correção {}", documentoFiscal.getId());
                processarDocumentosRecepcaoEvento(tRetConsSitNFe, documentoFiscal, TipoServicoEnum.CARTA_CORRECAO);

                return "processarConsultaProtocolo - xmlProcessadoCartaCorrecao";
            }
            return "processarConsultaProtocolo - Retorno de Carta de Correção não disponível";
        }

        return "processarConsultaProtocolo - Servico nao disponivel";
    }

    private String encaixarProtocolo(String xmlNFeSigned, TRetConsSitNFe tRetConsSitNFe,
            DocumentoFiscal documentoFiscal, String usuario) throws JAXBException, ParseException, Exception {
        LOGGER.info("encaixarProtocolo {} CSTAT {}", documentoFiscal.getId(), tRetConsSitNFe.getCStat());

        if ("217".equals(tRetConsSitNFe.getCStat())) {
            LOGGER.info("ConsultarProtocoloServiceImpl - NF-e não localizada na SEFAZ {}", documentoFiscal.getId());

            return tRetConsSitNFe.getCStat();

            // Caso tenha tido erro de Consumo Indevido, atualiza a datahora para não entrar na lista de documentos elegiveis
        } else if ("656".equals(tRetConsSitNFe.getCStat())) {
            LOGGER.info("ConsultarProtocoloServiceImpl - Consumo Indevido {}", documentoFiscal.getId());

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_INTERVALO_CONSUMO_INDEVIDO, 60));
            documentoFiscalRepository.updateDataHora(documentoFiscal.getId(), cal.getTime());

            return tRetConsSitNFe.getCStat();
        } else {
            TNFe tNFe = (TNFe) contextRetAutorizacao.createUnmarshaller().unmarshal(new StringSource(xmlNFeSigned));
            TProtNFe protNFe = tRetConsSitNFe.getProtNFe();

            TNfeProc nfeProc = new TNfeProc();
            nfeProc.setNFe(tNFe);
            nfeProc.setProtNFe(createProtocolo(protNFe));
            nfeProc.setVersao(protNFe.getVersao());

            StringResult stringResult = new StringResult();
            contextRetAutorizacao.createMarshaller().marshal(nfeProc, stringResult);
            String result = stringResult.toString();

            Long idClob = documentoClobRepository.insert(DocumentoClob.build(null, result, usuario));
            documentoEventoRepository.insert(DocumentoEvento.build(null, documentoFiscal.getId(), PontoDocumentoEnum.PROCESSADO.getCodigo(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), protNFe.getInfProt().getCStat(), idClob, usuario));

            String situacaoDocumento = documentoFiscal.getSituacaoDocumento();
            String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(Integer.valueOf(protNFe.getInfProt().getCStat()));
            //Cenarios possiveis de um retorno
            if ("110".equals(protNFe.getInfProt().getCStat())
                    || "301".equals(protNFe.getInfProt().getCStat())
                    || "302".equals(protNFe.getInfProt().getCStat())
                    || "303".equals(protNFe.getInfProt().getCStat())) {
                situacaoDocumento = SituacaoDocumentoEnum.DENEGADO.getCodigo();
            } else if ((SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                    || SituacaoDocumentoEnum.REJEITADO.getCodigo().equals(situacaoDocumento))
                    && CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
                if ("102".equals(protNFe.getInfProt().getCStat())) {
                    situacaoDocumento = SituacaoDocumentoEnum.INUTILIZADO.getCodigo();
                } else if ("100".equals(protNFe.getInfProt().getCStat())
                        || "124".equals(protNFe.getInfProt().getCStat())
                        || "136".equals(protNFe.getInfProt().getCStat())) {
                    situacaoDocumento = SituacaoDocumentoEnum.AUTORIZADO.getCodigo();
                }
            } else if ((SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                    || (SituacaoDocumentoEnum.AUTORIZADO.getCodigo().equals(situacaoDocumento)
                    && TipoEmissaoEnum.EPEC.getCodigo().toString().equals(nfeProc.getNFe().getInfNFe().getIde().getTpEmis())
                    && !"468".equals(protNFe.getInfProt().getCStat())))
                    && !CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
                situacaoDocumento = SituacaoDocumentoEnum.REJEITADO.getCodigo();
            } else if (SituacaoDocumentoEnum.AUTORIZADO.getCodigo().equals(situacaoDocumento)
                    && CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)
                    && "101".equals(protNFe.getInfProt().getCStat())) {
                situacaoDocumento = SituacaoDocumentoEnum.CANCELADO.getCodigo();
            }

            Long tipoEmissao = documentoFiscal.getTipoEmissao();
            if (tipoEmissao == null || tipoEmissao == 0) {
                tipoEmissao = Long.valueOf(tNFe.getInfNFe().getIde().getTpEmis());
            }

            documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(documentoFiscal.getId(), protNFe.getInfProt().getCStat(), PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, tipoEmissao, situacaoDocumento);

            //Grava a documento retorno apenas quando documento foi processado com sucesso
            if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())) {
                if (documentoRetornoRepository.findByIdDocFiscalAndTpServicoAndTpEvento(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null) == null) {
                    documentoRetornoRepository.insert(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null, idClob, usuario, usuario);
                } else {
                    documentoRetornoRepository.update(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null, idClob, usuario);
                }
            }

            return result;
        }
    }

    private com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe createProtocolo(TProtNFe protNFe) {

        com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe.InfProt infProt = new com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe.InfProt();
        infProt.setTpAmb(protNFe.getInfProt().getTpAmb());
        infProt.setVerAplic(protNFe.getInfProt().getVerAplic());
        infProt.setChNFe(protNFe.getInfProt().getChNFe());
        infProt.setDhRecbto(protNFe.getInfProt().getDhRecbto().toString());
        infProt.setNProt(protNFe.getInfProt().getNProt());
        infProt.setDigVal(protNFe.getInfProt().getDigVal());
        infProt.setCStat(protNFe.getInfProt().getCStat());
        infProt.setXMotivo(protNFe.getInfProt().getXMotivo());

        com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe tProtNFe = new com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe();
        tProtNFe.setInfProt(infProt);

        return tProtNFe;
    }

    protected void processarDocumentosRecepcaoEvento(TRetConsSitNFe tRetConsSitNFe, DocumentoFiscal docu, TipoServicoEnum tipoServico) throws Exception {
        LOGGER.info("processarDocumentosRecepcaoEvento {} ", docu.getId());

        String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

        com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TRetEvento tRetEvento = tRetConsSitNFe.getProcEventoNFe().iterator().next().getRetEvento();

        StringResult xmlProcessado = new StringResult();
        contextConsultaProtocolo.createMarshaller().marshal(tRetEvento, xmlProcessado);

        Long idClob = documentoClobRepository.insert(DocumentoClob.build(null, xmlProcessado.toString(), usuario));

        documentoEventoRepository.insert(DocumentoEvento.build(null, docu.getId(), PontoDocumentoEnum.PROCESSADO.getCodigo(), tipoServico.getTipoRetorno(), tRetEvento.getInfEvento().getCStat(), idClob, usuario));

        String situacaoDocumento = docu.getSituacaoDocumento();
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

        documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(docu.getId(), tRetEvento.getInfEvento().getCStat(), PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, null, situacaoDocumento);

        //Grava a documento retorno apenas quando documento foi processado com sucesso
        if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())) {
            Long tipoEvento = null;
            if (TipoServicoEnum.MANIFESTACAO.getTipoRetorno().equals(tipoServico.getTipoRetorno())) {
                tipoEvento = Long.valueOf(tRetEvento.getInfEvento().getTpEvento());
            }

            if (documentoRetornoRepository.findByIdDocFiscalAndTpServicoAndTpEvento(docu.getId(), tipoServico.getTipoRetorno(), tipoEvento) == null) {
                documentoRetornoRepository.insert(docu.getId(), tipoServico.getTipoRetorno(), tipoEvento, idClob, usuario, usuario);
            } else {
                documentoRetornoRepository.update(docu.getId(), tipoServico.getTipoRetorno(), tipoEvento, idClob, usuario);
            }
        }

        kafkaProducerService.invokeCallback(docu, xmlProcessado.toString(), tipoServico);
    }

    protected void emitirNovoLote(TRetConsSitNFe tRetConsSitNFe, DocumentoFiscal docu, Integer tipoEmissaoAtual, ServicosEnum servico, DocumentoClob docClob) throws ParseException {
        LOGGER.info("emitirNovoLote {} ", docu.getId());

        if (docClob != null) {
            emitirLoteService.emitirLote(ResumoDocumentoFiscal.build(
                    docu.getId(),
                    docu.getTipoDocumentoFiscal(),
                    docu.getIdEmissor(),
                    Integer.valueOf(tRetConsSitNFe.getCUF()),
                    docu.getIdMunicipio(),
                    tipoEmissaoAtual,
                    servico.getVersao(),
                    docClob.getId(),
                    docClob.getClob().length()),
                    servico, false);
        }
    }

    protected DocumentoClob assinarDocumento(DocumentoFiscal documentoFiscal) throws Exception {
        LOGGER.info("emitirLoteDocumentosAbertosParados (scheduled) - assinarDocumento {}", documentoFiscal.getId());

        String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

        DocumentoClob documentoClob = documentoClobRepository.getLastXmlByIdDocFiscalAndPontoDocumento(documentoFiscal.getId(), PontoDocumentoEnum.RECEBIDO_XML_RAW);

        Document docNFe = XMLUtils.convertStringToDocument(documentoClob.getClob());

        // Certificado
        long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(Long.valueOf(documentoFiscal.getIdEmissor()));
        CertificadoDigital certificado = certificadoDigitalService.getByIdEmissorRaiz(raizCnpjEmitente);

        FazemuUtils.signXml(docNFe, certificado, ServicosEnum.AUTORIZACAO_NFE);

        Long idDocumentoXmlSignedClob = documentoClobRepository.insert(DocumentoClob.build(null, XMLUtils.convertDocumentToString(docNFe), usuario));
        documentoEventoRepository.insert(DocumentoEvento.build(null, documentoFiscal.getId(), PontoDocumentoEnum.RECEBIDO_XML_SIGNED.getCodigo(), null, null, idDocumentoXmlSignedClob, usuario));

        return documentoClobRepository.findById(idDocumentoXmlSignedClob);
    }

}
