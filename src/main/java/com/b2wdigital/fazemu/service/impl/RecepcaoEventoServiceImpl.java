package com.b2wdigital.fazemu.service.impl;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.b2wdigital.assinatura_digital.CertificadoDigital;
import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoEventoRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.repository.EmissorRaizCertificadoDigitalRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.business.service.EmitirLoteService;
import com.b2wdigital.fazemu.business.service.RecepcaoEventoService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.EmissorRaiz;
import com.b2wdigital.fazemu.domain.EmissorRaizCertificadoDigital;
import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.domain.ResumoDocumentoFiscal;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.RecepcaoEventoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;
import com.b2wdigital.fazemu.exception.FazemuDAOException;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2winc.corpserv.message.exception.ConflictException;

import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recepcao de Evento Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class RecepcaoEventoServiceImpl extends AbstractNFeServiceImpl implements RecepcaoEventoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecepcaoEventoServiceImpl.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private EmissorRaizCertificadoDigitalRepository emissorRaizCertificadoDigitalRepository;

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoEventoRepository documentoEventoRepository;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private EmitirLoteService emitirLoteService;

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Autowired
    @Qualifier("nfeRecepcaoEventoContext")
    private JAXBContext contextRecepcaoEvento;

    @Autowired
    @Qualifier("nfeRecepcaoEventoCancelamentoContext")
    private JAXBContext contextCancelamento;

    @Autowired
    @Qualifier("nfeRecepcaoEventoCartaCorrecaoContext")
    private JAXBContext contextCartaCorrecao;

    @Autowired
    @Qualifier("nfeRecepcaoEventoManifestacaoContext")
    private JAXBContext contextManifestacao;

    @Autowired
    @Qualifier("nfeRecepcaoEventoEpecContext")
    private JAXBContext contextEpec;

    @Autowired
    private RedisOperationsService redisOperationsService;

    /**
     * process
     *
     * @param request
     * @param usuario
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NfeResultMsg process(NfeDadosMsg request, String usuario) throws Exception {
        ServicosEnum servico = null;
        try {
            LOGGER.debug("RecepcaoEventoServiceImpl: process");

            String statusFazemu = (String) redisOperationsService.getKeyValue(FazemuUtils.FAZEMU_NFE_STATUS);
            if (!"ON".equals(statusFazemu)) {
                throw new FazemuServiceException("Sistema Indisponível no momento.");
            }

            // Obtem documento e callback se houver
            Map<String, Document> map = getDocument(request);
            Document docEvento = map.get("xml");

            // retira namespace
            XMLUtils.cleanNameSpace(docEvento);

            // Converte para String e retira espacos e quebras de linha
            String xmlFinal = XMLUtils.convertDocumentToString(docEvento);

            // Converte para Document
            Document docFinal = XMLUtils.convertStringToDocument(xmlFinal);
            docFinal.setXmlStandalone(true);

            servico = getServico(docFinal);

            // Caso seja manifestacao, obrigatorio alterar uf para 91 (AN)
            if (ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO.equals(servico)) {
                Document docManifestacao = getManifestacaoDocument(docFinal);
                docFinal = docManifestacao;
            }

            Document docCallback = map.get("callback");
            ResumoLote lote = preValidations(docFinal, servico, docCallback);

            // Certificado
            long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(Long.valueOf(lote.getIdEmissor()));
            CertificadoDigital certificado = certificadoDigitalService.getByIdEmissorRaiz(raizCnpjEmitente);

            FazemuUtils.signXml(docFinal, certificado, servico);

            String successMessage = "";
            String successCode = "";
            PontoDocumentoEnum pontoDocumento = null;
            if (ServicosEnum.RECEPCAO_EVENTO_CANCELAMENTO.equals(servico)) {
                successMessage = FazemuUtils.MSG_002_CANCELAMENTO_ACCEPTED;
                successCode = FazemuUtils.CODE_002_CANCELAMENTO_ACCEPTED;
                pontoDocumento = PontoDocumentoEnum.CANCELAMENTO;

            } else if (ServicosEnum.RECEPCAO_EVENTO_CARTA_CORRECAO.equals(servico)) {
                successMessage = FazemuUtils.MSG_003_CARTA_CORRECAO_ACCEPTED;
                successCode = FazemuUtils.CODE_003_CARTA_CORRECAO_ACCEPTED;
                pontoDocumento = PontoDocumentoEnum.CARTA_CORRECAO;

            } else if (ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO.equals(servico)) {
                successMessage = FazemuUtils.MSG_004_MANIFESTACAO_ACCEPTED;
                successCode = FazemuUtils.CODE_004_MANIFESTACAO_ACCEPTED;
                pontoDocumento = PontoDocumentoEnum.MANIFESTACAO;

            } else if (ServicosEnum.RECEPCAO_EVENTO_EPEC.equals(servico)) {
                successMessage = FazemuUtils.MSG_005_EPEC_ACCEPTED;
                successCode = FazemuUtils.CODE_005_EPEC_ACCEPTED;
                pontoDocumento = PontoDocumentoEnum.RECEBIDO_XML_EPEC;
            }

            persist(lote, docFinal, servico, pontoDocumento, usuario);

            return createResultMessage(FazemuUtils.PREFIX + successCode, successMessage, servico);

        } catch (ConflictException e) {
            throw e;

        } catch (Exception e) {
            return createResultMessage(FazemuUtils.PREFIX + FazemuUtils.ERROR_CODE, e.getMessage(), servico);
        }

    }

    protected Map<String, Document> getDocument(NfeDadosMsg nfeDadosMsg) throws JAXBException, ParserConfigurationException {

        Map<String, Document> map = new HashMap<>();

        //xml
        Document document = XMLUtils.createNewDocument();
        contextRecepcaoEvento.createMarshaller().marshal(nfeDadosMsg, document);

        //get main tag
        Node node = document.getFirstChild().getChildNodes().item(0);
        Document doc = XMLUtils.createNewDocument();
        Node importedNode = doc.importNode(node, true);
        doc.appendChild(importedNode);
        map.put("xml", doc);

        //get callback tag
        try {
            Node nodeCallback = document.getFirstChild().getChildNodes().item(1);
            Document docCallback = XMLUtils.createNewDocument();
            Node importedNodeCallback = docCallback.importNode(nodeCallback, true);
            docCallback.appendChild(importedNodeCallback);
            map.put("callback", docCallback);
        } catch (Exception e) {
            // DO NOTHING
        }

        return map;
    }

    protected ResumoLote preValidations(Document document, ServicosEnum servico, Document docCallback) throws JAXBException, ConflictException {
        ResumoLote lote = new ResumoLote();
        lote.setTipoDocumentoFiscal(TIPO_DOCUMENTO_FISCAL_NFE);

        if (ServicosEnum.RECEPCAO_EVENTO_CANCELAMENTO.equals(servico)) {
            com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TEvento tEvento = (com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TEvento) contextCancelamento.createUnmarshaller().unmarshal(document);

            lote.setChaveAcesso(tEvento.getInfEvento().getChNFe());
            lote.setUf(Integer.valueOf(tEvento.getInfEvento().getCOrgao()));
            lote.setIdEmissor(Long.valueOf(tEvento.getInfEvento().getCNPJ()));
            lote.setVersao(tEvento.getVersao());

            //Verifica informacoes especificas
            if (StringUtils.isBlank(tEvento.getInfEvento().getDetEvento().getDescEvento())) {
                throw new FazemuServiceException("Descrição do Evento não encontrada no XML");
            }
            if (StringUtils.isBlank(tEvento.getInfEvento().getDetEvento().getNProt())) {
                throw new FazemuServiceException("Número de Protocolo não encontrado no XML");
            }
            if (StringUtils.isBlank(tEvento.getInfEvento().getDetEvento().getXJust())) {
                throw new FazemuServiceException("Justificativa não encontrada no XML");
            }

        } else if (ServicosEnum.RECEPCAO_EVENTO_CARTA_CORRECAO.equals(servico)) {
            com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TEvento tEvento = (com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TEvento) contextCartaCorrecao.createUnmarshaller().unmarshal(document);

            lote.setChaveAcesso(tEvento.getInfEvento().getChNFe());
            lote.setUf(Integer.valueOf(tEvento.getInfEvento().getCOrgao()));
            lote.setIdEmissor(Long.valueOf(tEvento.getInfEvento().getCNPJ()));
            lote.setVersao(tEvento.getVersao());

            if (StringUtils.isBlank(tEvento.getInfEvento().getDetEvento().getDescEvento())) {
                throw new FazemuServiceException("Descrição do Evento não encontrada no XML");
            }
            if (StringUtils.isBlank(tEvento.getInfEvento().getDetEvento().getXCorrecao())) {
                throw new FazemuServiceException("Correção não encontrado no XML");
            }
            if (StringUtils.isBlank(tEvento.getInfEvento().getDetEvento().getXCondUso())) {
                throw new FazemuServiceException("Condição de Uso não encontrada no XML");
            }

        } else if (ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO.equals(servico)) {
            com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TEvento tEvento = (com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TEvento) contextManifestacao.createUnmarshaller().unmarshal(document);

            manifestacaoResolver(document, tEvento, docCallback);

            lote.setChaveAcesso(tEvento.getInfEvento().getChNFe());
            lote.setUf(Integer.valueOf(tEvento.getInfEvento().getCOrgao()));
            lote.setIdEmissor(Long.valueOf(tEvento.getInfEvento().getCNPJ()));
            lote.setVersao(tEvento.getVersao());

            if (StringUtils.isBlank(tEvento.getInfEvento().getDetEvento().getDescEvento())) {
                throw new FazemuServiceException("Descrição do Evento não encontrada no XML");
            }

        } else if (ServicosEnum.RECEPCAO_EVENTO_EPEC.equals(servico)) {
            com.b2wdigital.nfe.schema.v4v160b.epec.EPEC_v1.TEvento tEvento = (com.b2wdigital.nfe.schema.v4v160b.epec.EPEC_v1.TEvento) contextEpec.createUnmarshaller().unmarshal(document);

            lote.setChaveAcesso(tEvento.getInfEvento().getChNFe());
            lote.setUf(Integer.valueOf(tEvento.getInfEvento().getCOrgao()));
            lote.setIdEmissor(Long.valueOf(tEvento.getInfEvento().getCNPJ()));
            lote.setVersao(tEvento.getVersao());

        } else {
            throw new FazemuServiceException("Tipo de serviço desconhecido");
        }

        Integer tipoEmissao = parametrosInfraRepository.getAsInteger(null, ParametrosInfraRepository.PAIN_TP_EMISSAO);
        lote.setTipoEmissao(tipoEmissao);

        // Verifica informacoes obrigatorias
        if (StringUtils.isBlank(lote.getChaveAcesso())) {
            throw new FazemuServiceException("Chave de Acesso não encontrada no XML");
        }
        if (StringUtils.isBlank(String.valueOf(lote.getUf()))) {
            throw new FazemuServiceException("Codigo Orgao não encontrado no XML");
        }
        if (StringUtils.isBlank(String.valueOf(lote.getIdEmissor()))) {
            throw new FazemuServiceException("CNPJ não encontrado no XML");
        }

        // Valida chave acesso
        if (!ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO.equals(servico)) {
            try {
                ChaveAcessoNFe key = ChaveAcessoNFe.unparseKey(lote.getChaveAcesso());
                if (!key.cUF.equals(StringUtils.leftPad(String.valueOf(lote.getUf()), 2, "0"))) {
                    throw new FazemuServiceException("UF Emitente inconsistente com a chave de acesso");
                }
                if (!key.cnpjCpf.equals(StringUtils.leftPad(String.valueOf(lote.getIdEmissor()).replaceAll("\\D", ""), 14, "0"))) {
                    throw new FazemuServiceException("CNPJ inconsistente com a chave de acesso");
                }

            } catch (org.apache.el.parser.ParseException e) {
                throw new FazemuServiceException("Chave de Acesso invalida");
            }
        }

        // Verifica se UF Emitente esta cadastrado
        Estado estado = findEstadoByCodigoIbge(lote.getUf());
        if (estado == null) {
            throw new FazemuServiceException("Codigo do Orgao não cadastrado: " + lote.getUf());
        }

        // Verifica se CNPJ Emitente esta cadastrado
        long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(Long.valueOf(lote.getIdEmissor()));
        if (!ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO.equals(servico)) {
            EmissorRaiz emissor = findEmissorRaizById(String.valueOf(raizCnpjEmitente));
            if (emissor == null) {
                throw new FazemuServiceException("Emitente não cadastrado: " + lote.getIdEmissor());
            }
        }

        //Verifica se existe certificado para o CNPJ emissor
        EmissorRaizCertificadoDigital emissorRaizCertificadoDigital = emissorRaizCertificadoDigitalRepository.findByIdEmissorRaiz(raizCnpjEmitente);
        if (emissorRaizCertificadoDigital == null || emissorRaizCertificadoDigital.getCertificadoBytes() == null) {
            throw new FazemuServiceException("Certificado Digital não cadastrado para o Emitente: " + lote.getIdEmissor());
        }

        // Verifica se existe documento cadastrado
        try {
            DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(lote.getChaveAcesso());

            if (documentoFiscal != null) {
                // Verifica se a NFe já foi recebida e está com situacao aberta
                if (documentoFiscal.getSituacao() != null && SituacaoEnum.ABERTO.getCodigo().equals(documentoFiscal.getSituacao())) {
                    throw new FazemuServiceException("NFe " + lote.getChaveAcesso() + " em processo de aprovação.");
                }

                // Não permite operacao caso nfe esteja em aguardando conciliação (CSTAT 468)
                if (documentoFiscal.getSituacaoAutorizador() != null && "468".contentEquals(documentoFiscal.getSituacaoAutorizador())) {
                    throw new FazemuServiceException("NFe em processo de conciliação após EPEC: " + lote.getChaveAcesso());
                }
            }

        } catch (FazemuDAOException e) {
            throw new FazemuServiceException("Erro ao buscar documento fiscal para chave: " + lote.getChaveAcesso(), e);
        }

        return lote;
    }

    //Na manifestacao devera ser criado um documento fiscal anteriormente ao processo
    protected void manifestacaoResolver(Document docFinal, com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TEvento tEvento, Document docCallback) {
        String chaveAcesso = tEvento.getInfEvento().getChNFe();

        // Valida chave acesso
        ChaveAcessoNFe chaveAcessoNFe;
        try {
            chaveAcessoNFe = ChaveAcessoNFe.unparseKey(chaveAcesso);
        } catch (org.apache.el.parser.ParseException e) {
            throw new FazemuServiceException("Chave de Acesso invalida");
        }

        // Verificar se existe callback
        String sistemaWs = null;
        if (docCallback != null) {
            XMLUtils.cleanNameSpace(docCallback);
            sistemaWs = docCallback.getFirstChild().getTextContent();
        }

        // Verifica se existe documento cadastrado
        try {
            DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);

            if (documentoFiscal == null) {
                String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

                documentoFiscal = new DocumentoFiscal();
                documentoFiscal.setTipoDocumentoFiscal(TIPO_DOCUMENTO_FISCAL_NFE);
                documentoFiscal.setIdEmissor(Long.parseLong(chaveAcessoNFe.cnpjCpf));
                documentoFiscal.setNumeroDocumentoFiscal(Long.parseLong(chaveAcessoNFe.nNF));
                documentoFiscal.setSerieDocumentoFiscal(Long.parseLong(chaveAcessoNFe.serie));
                documentoFiscal.setDataHoraEmissao(null);
                Integer codigoIbge = Integer.valueOf(chaveAcessoNFe.cUF);
                documentoFiscal.setIdEstado(estadoRepository.findByCodigoIbge(codigoIbge).getId());
                documentoFiscal.setVersao(tEvento.getInfEvento().getVerEvento());
                documentoFiscal.setChaveAcesso(chaveAcesso);
                documentoFiscal.setChaveAcessoEnviada(chaveAcesso);
                documentoFiscal.setIdPonto(PontoDocumentoEnum.RECEBIDO_XML_MANIFESTACAO.getCodigo());
                documentoFiscal.setSituacaoAutorizador(null);
                documentoFiscal.setSituacaoDocumento(SituacaoDocumentoEnum.AUTORIZADO.getCodigo());
                documentoFiscal.setSituacao(SituacaoEnum.LIQUIDADO.getCodigo());
                documentoFiscal.setAnoDocumentoFiscal(FazemuUtils.getFullYearFormat(Integer.valueOf(chaveAcessoNFe.dataAAMM.substring(0, 2))));
                documentoFiscal.setIdDestinatario(Long.parseLong(tEvento.getInfEvento().getCNPJ()));
                documentoFiscal.setIdSistema(sistemaWs != null ? sistemaWs : StringUtils.EMPTY);
                documentoFiscal.setUsuarioReg(usuario);
                documentoFiscal.setUsuario(usuario);

                Long idDocumentoFiscal = documentoFiscalRepository.insert(documentoFiscal);

                String xmlManifestacao = XMLUtils.convertDocumentToString(docFinal);

                Long idDocumentoXmlClob = documentoClobRepository.insert(DocumentoClob.build(null, xmlManifestacao, usuario));
                documentoEventoRepository.insert(DocumentoEvento.build(null, idDocumentoFiscal, PontoDocumentoEnum.RECEBIDO_XML_MANIFESTACAO.getCodigo(), null, null, idDocumentoXmlClob, usuario));
            }

        } catch (FazemuDAOException e) {
            throw new FazemuServiceException("Erro ao buscar documento fiscal para chave: " + chaveAcesso, e);
        }
    }

    protected ServicosEnum getServico(Document document) {

        NodeList nodeList = document.getElementsByTagName("tpEvento");
        if (nodeList == null) {
            throw new FazemuServiceException("Tipo de Evento não encontrado no XML");
        }

        Element element = (Element) nodeList.item(0);
        Integer tipoEvento = Integer.valueOf(element.getTextContent());

        if (RecepcaoEventoEnum.CANCELAMENTO.getCodigoEvento().equals(tipoEvento)) {
            return ServicosEnum.RECEPCAO_EVENTO_CANCELAMENTO;

        } else if (RecepcaoEventoEnum.CARTA_CORRECAO.getCodigoEvento().equals(tipoEvento)) {
            return ServicosEnum.RECEPCAO_EVENTO_CARTA_CORRECAO;

        } else if (RecepcaoEventoEnum.MANIFESTACAO_CONFIRMACAO_OPERACAO.getCodigoEvento().equals(tipoEvento)
                || RecepcaoEventoEnum.MANIFESTACAO_CIENCIA_OPERACAO.getCodigoEvento().equals(tipoEvento)
                || RecepcaoEventoEnum.MANIFESTACAO_DESCONHECIMENTO_OPERACAO.getCodigoEvento().equals(tipoEvento)
                || RecepcaoEventoEnum.MANIFESTACAO_OPERACAO_NAO_REALIZADA.getCodigoEvento().equals(tipoEvento)) {
            return ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO;

        } else if (RecepcaoEventoEnum.EPEC.getCodigoEvento().equals(tipoEvento)) {
            return ServicosEnum.RECEPCAO_EVENTO_EPEC;

        } else {
            throw new FazemuServiceException("Tipo de Evento não conhecido: " + tipoEvento);
        }

    }

    protected void persist(ResumoLote lote, Document docFinal, ServicosEnum servico, PontoDocumentoEnum pontoDocumento, String usuario) throws JAXBException, ParseException {

        if (usuario == null) {
            usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);
        }

        String xmlFinal = XMLUtils.convertDocumentToString(docFinal);

        DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(lote.getChaveAcesso());
        if (documentoFiscal == null) {
            throw new FazemuServiceException("Documento Fiscal nao encontrado com a chave: " + lote.getChaveAcesso());
        }

        documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(documentoFiscal.getId(), null, pontoDocumento, SituacaoEnum.ABERTO, null, null);

        Long idDocumentoClob = documentoClobRepository.insert(DocumentoClob.build(null, xmlFinal, usuario));
        documentoEventoRepository.insert(DocumentoEvento.build(null, documentoFiscal.getId(), pontoDocumento.getCodigo(), null, null, idDocumentoClob, usuario));

        emitirLoteService.emitirLote(ResumoDocumentoFiscal.build(documentoFiscal.getId(),
                documentoFiscal.getTipoDocumentoFiscal(),
                lote.getIdEmissor(),
                lote.getUf(),
                lote.getMunicipio(),
                lote.getTipoEmissao(),
                servico.getVersao(),
                idDocumentoClob,
                xmlFinal.length()),
                servico,
                false);
    }

    protected Document getManifestacaoDocument(Document docFinal) throws Exception {
        com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TEvento tEvento = (com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TEvento) contextManifestacao.createUnmarshaller().unmarshal(docFinal);
        tEvento.getInfEvento().setCOrgao("91");

        Document document = XMLUtils.createNewDocument();
        contextManifestacao.createMarshaller().marshal(tEvento, document);

        // retira namespace
        XMLUtils.cleanNameSpace(document);
        XMLUtils.insertNameSpace(document);

        // Converte para String e retira espacos e quebras de linha
        String xmlManifestacao = XMLUtils.convertDocumentToString(document);

        // Converte para Document
        Document docManifestacao = XMLUtils.convertStringToDocument(xmlManifestacao);
        docManifestacao.setXmlStandalone(true);

        return docManifestacao;
    }

    protected NfeResultMsg createResultMessage(String code, String message, ServicosEnum servico) throws ParserConfigurationException, JAXBException {

        String TIPO_AMBIENTE = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE);
        String APLICACAO = FazemuUtils.APLICACAO;
        String CODIGO_RETORNO = code;

        // Create the Document
        Document document = XMLUtils.createNewDocument();

        if (ServicosEnum.RECEPCAO_EVENTO_CANCELAMENTO.equals(servico)) {
            com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TRetEnvEvento retorno = new com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TRetEnvEvento();
            retorno.setVersao(servico.getVersao());
            retorno.setTpAmb(TIPO_AMBIENTE);
            retorno.setVerAplic(APLICACAO);
            retorno.setCStat(CODIGO_RETORNO);
            retorno.setXMotivo(message);

            contextCancelamento.createMarshaller().marshal(retorno, document);

        } else if (ServicosEnum.RECEPCAO_EVENTO_CARTA_CORRECAO.equals(servico)) {
            com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TRetEnvEvento retorno = new com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TRetEnvEvento();
            retorno.setVersao(servico.getVersao());
            retorno.setTpAmb(TIPO_AMBIENTE);
            retorno.setVerAplic(APLICACAO);
            retorno.setCStat(CODIGO_RETORNO);
            retorno.setXMotivo(message);

            contextCartaCorrecao.createMarshaller().marshal(retorno, document);

        } else if (ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO.equals(servico)) {
            com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TRetEnvEvento retorno = new com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TRetEnvEvento();
            retorno.setVersao(servico.getVersao());
            retorno.setTpAmb(TIPO_AMBIENTE);
            retorno.setVerAplic(APLICACAO);
            retorno.setCStat(CODIGO_RETORNO);
            retorno.setXMotivo(message);

            contextManifestacao.createMarshaller().marshal(retorno, document);

        } else if (ServicosEnum.RECEPCAO_EVENTO_EPEC.equals(servico)) {
            com.b2wdigital.nfe.schema.v4v160b.epec.EPEC_v1.TRetEnvEvento retorno = new com.b2wdigital.nfe.schema.v4v160b.epec.EPEC_v1.TRetEnvEvento();
            retorno.setVersao(servico.getVersao());
            retorno.setTpAmb(TIPO_AMBIENTE);
            retorno.setVerAplic(APLICACAO);
            retorno.setCStat(CODIGO_RETORNO);
            retorno.setXMotivo(message);

            contextEpec.createMarshaller().marshal(retorno, document);
        }

        NfeResultMsg nfeResultMsg = new NfeResultMsg();
        nfeResultMsg.getContent().add(document.getDocumentElement());

        return nfeResultMsg;
    }

}
