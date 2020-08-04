package com.b2wdigital.fazemu.service.impl;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.b2wdigital.assinatura_digital.CertificadoDigital;
import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoEventoRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.repository.EmissorRaizCertificadoDigitalRepository;
import com.b2wdigital.fazemu.business.repository.EstadoConfiguracaoRepository;
import com.b2wdigital.fazemu.business.repository.EstadoTipoEmissaoRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.repository.ResponsavelTecnicoRepository;
import com.b2wdigital.fazemu.business.service.AutorizacaoNFeService;
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.business.service.EmitirLoteService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.EmissorRaiz;
import com.b2wdigital.fazemu.domain.EmissorRaizCertificadoDigital;
import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.domain.EstadoConfiguracao;
import com.b2wdigital.fazemu.domain.EstadoTipoEmissao;
import com.b2wdigital.fazemu.domain.ResponsavelTecnico;
import com.b2wdigital.fazemu.domain.ResumoDocumentoFiscal;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.RecepcaoEventoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;
import com.b2wdigital.fazemu.enumeration.TipoEmissaoEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;
import com.b2wdigital.fazemu.utils.DateUtils;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TNFe;
import com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TNFe.InfNFe.InfAdic.ObsCont;
import com.b2wdigital.nfe.schema.v4v160b.epec.EPEC_v1.TEvento;
import com.b2wdigital.nfe.schema.v4v160b.epec.EPEC_v1.TUf;

import br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeDadosMsg;
import org.springframework.transaction.annotation.Transactional;

/**
 * AutorizacaoNFe Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class AutorizacaoNFeServiceImpl extends AbstractNFeServiceImpl implements AutorizacaoNFeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutorizacaoNFeServiceImpl.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    public static final String MAPA_XML_RAW = "RAW";
    public static final String MAPA_XML_SIGNED = "SIGNED";
    public static final String MAPA_XML_EPEC = "EPEC";

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoEventoRepository documentoEventoRepository;

    @Autowired
    private EmissorRaizCertificadoDigitalRepository emissorRaizCertificadoDigitalRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private ResponsavelTecnicoRepository responsavelTecnicoRepository;

    @Autowired
    private EstadoConfiguracaoRepository estadoConfiguracaoRepository;

    @Autowired
    private EstadoTipoEmissaoRepository estadoTipoEmissaoRepository;

    @Autowired
    private EmitirLoteService emitirLoteService;

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Autowired
    @Qualifier("nfeAutorizacaoContext")
    private JAXBContext context;

    @Autowired
    @Qualifier("nfeRecepcaoEventoEpecContext")
    private JAXBContext contextEpec;

    /**
     * process
     *
     * @param request
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(NfeDadosMsg request) throws Exception {
        try {
            LOGGER.debug("AutorizacaoNFeServiceImpl: process");

            String statusFazemu = (String) redisOperationsService.getKeyValue(FazemuUtils.FAZEMU_NFE_STATUS);
            if (!"ON".equals(statusFazemu)) {
                throw new FazemuServiceException("Sistema Indisponível no momento.");
            }

            Map<String, String> mapXml = new HashMap<>();

            // Obtem documento nfe
            Document docNFe = getDocument(request);
            
            // retira namespace
            XMLUtils.cleanNameSpace(docNFe);

            XMLUtils.showXML(docNFe);
            
            // Converte para String e retira espacos e quebras de linha
            String xmlRaw = XMLUtils.convertDocumentToString(docNFe, true);
            mapXml.put(MAPA_XML_RAW, xmlRaw);

            // Converte para objeto NFe
            final TNFe tNFe = (TNFe) context.createUnmarshaller().unmarshal(docNFe);

            String xmlFinal = incluiResponsavelTecnico(xmlRaw, FazemuUtils.obterRaizCNPJ(Long.valueOf(tNFe.getInfNFe().getEmit().getCNPJ())), tNFe.getInfNFe().getIde().getCUF());

            // Converte para Document
            Document docFinal = XMLUtils.convertStringToDocument(xmlFinal);
            docFinal.setXmlStandalone(true);

            String chaveAcessoOriginal = FazemuUtils.normalizarChaveAcesso(tNFe.getInfNFe().getId());
            preValidations(tNFe);

            //Determina o tipo de emissao
            Integer tipoEmissaoAtual = getTipoEmissao(tNFe.getInfNFe().getIde().getCUF());

            ChaveAcessoNFe chaveAcessoAlterada = null;
            if (TipoEmissaoEnum.EPEC.getCodigo().equals(tipoEmissaoAtual)) {
                // Chave com novo tipo de emissao
                chaveAcessoAlterada = getNewChaveAcesso(tNFe.getInfNFe().getId(), tipoEmissaoAtual);

                Document docEpec = getEpecDocument(tNFe, tipoEmissaoAtual, ChaveAcessoNFe.parseKey(chaveAcessoAlterada, true));
                mapXml.put(MAPA_XML_EPEC, XMLUtils.convertDocumentToString(docEpec, true));

                docFinal = getContingenciaDocument(tNFe, tipoEmissaoAtual, chaveAcessoAlterada);

            } else if (TipoEmissaoEnum.FS_DA.getCodigo().equals(tipoEmissaoAtual)
                    || TipoEmissaoEnum.SVC_AN.getCodigo().equals(tipoEmissaoAtual)
                    || TipoEmissaoEnum.SVC_RS.getCodigo().equals(tipoEmissaoAtual)) {

                // Chave com novo tipo de emissao
                chaveAcessoAlterada = getNewChaveAcesso(tNFe.getInfNFe().getId(), tipoEmissaoAtual);

                // Altera versao final do document
                docFinal = getContingenciaDocument(tNFe, tipoEmissaoAtual, chaveAcessoAlterada);
            }

            // Certificado
            long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(Long.valueOf(tNFe.getInfNFe().getEmit().getCNPJ()));
            CertificadoDigital certificado = certificadoDigitalService.getByIdEmissorRaiz(raizCnpjEmitente);

            FazemuUtils.signXml(docFinal, certificado, ServicosEnum.AUTORIZACAO_NFE);

            mapXml.put(MAPA_XML_SIGNED, XMLUtils.convertDocumentToString(docFinal, true));

            persist(tNFe, mapXml, tipoEmissaoAtual, chaveAcessoOriginal, chaveAcessoAlterada);

        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    protected Document getDocument(NfeDadosMsg nfeDadosMsg) throws JAXBException, ParserConfigurationException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        context.createMarshaller().marshal(nfeDadosMsg, document);

        // Delete nfeDadosMsg
        Node node = document.getFirstChild().getChildNodes().item(0);
        factory.setNamespaceAware(true);
        Document newDocument = builder.newDocument();
        Node importedNode = newDocument.importNode(node, true);
        newDocument.appendChild(importedNode);

        return newDocument;
    }

    protected void preValidations(TNFe tNFe) {

        String chaveAcesso = FazemuUtils.normalizarChaveAcesso(tNFe.getInfNFe().getId());
        String ufEmitente = tNFe.getInfNFe().getIde().getCUF();
        String dataEmissao = tNFe.getInfNFe().getIde().getDhEmi();
        String cnpjEmitente = tNFe.getInfNFe().getEmit().getCNPJ();
        String modeloDocumentoFiscal = tNFe.getInfNFe().getIde().getMod();
        String serieDocumentoFiscal = tNFe.getInfNFe().getIde().getSerie();
        String numeroDocumentoFiscal = tNFe.getInfNFe().getIde().getNNF();
        String tipoEmissao = tNFe.getInfNFe().getIde().getTpEmis();
        String codigoChaveAcesso = tNFe.getInfNFe().getIde().getCNF();
        String digitoChaveAcesso = tNFe.getInfNFe().getIde().getCDV();
        String versao = tNFe.getInfNFe().getVersao();

        // Verifica informacoes obrigatorias
        if (StringUtils.isBlank(chaveAcesso)) {
            throw new FazemuServiceException("Chave de Acesso não encontrada no XML");
        }
        if (StringUtils.isBlank(ufEmitente)) {
            throw new FazemuServiceException("UF não encontrado no XML");
        }
        if (StringUtils.isBlank(dataEmissao)) {
            throw new FazemuServiceException("Data Hora Emissao não encontrada no XML");
        }
        if (StringUtils.isBlank(cnpjEmitente)) {
            throw new FazemuServiceException("CNPJ Emitente não encontrado no XML");
        }
        if (StringUtils.isBlank(modeloDocumentoFiscal)) {
            throw new FazemuServiceException("Modelo de Documento Fiscal não encontrada no XML");
        }
        if (StringUtils.isBlank(serieDocumentoFiscal)) {
            throw new FazemuServiceException("Serie de Documento Fiscal não encontrada no XML");
        }
        if (StringUtils.isBlank(numeroDocumentoFiscal)) {
            throw new FazemuServiceException("Numero de Documento Fiscal não encontrado no XML");
        }
        if (StringUtils.isBlank(tipoEmissao)) {
            throw new FazemuServiceException("Tipo de Emissao não encontrado no XML");
        }
        if (StringUtils.isBlank(codigoChaveAcesso)) {
            throw new FazemuServiceException("Codigo Numerico da Chave de Acesso não encontrado no XML");
        }
        if (StringUtils.isBlank(digitoChaveAcesso)) {
            throw new FazemuServiceException("Digito Verificador da Chave de Acesso não encontrado no XML");
        }
        if (StringUtils.isBlank(versao)) {
            throw new FazemuServiceException("Versao não encontrada no XML");
        }

        // Valida chave acesso
        try {
            ChaveAcessoNFe key = ChaveAcessoNFe.unparseKey(chaveAcesso);

            if (!key.cUF.equals(StringUtils.leftPad(ufEmitente, 2, "0"))) {
                throw new FazemuServiceException("UF Emitente inconsistente com a chave de acesso");
            }
            if (!key.cnpjCpf.equals(StringUtils.leftPad(cnpjEmitente.replaceAll("\\D", ""), 14, "0"))) {
                throw new FazemuServiceException("CNPJ Emitente inconsistente com a chave de acesso");
            }
            if (!key.mod.equals(StringUtils.leftPad(modeloDocumentoFiscal, 2, "0"))) {
                throw new FazemuServiceException("Modelo de Documento Fiscal inconsistente com a chave de acesso");
            }
            if (!key.serie.equals(StringUtils.leftPad(serieDocumentoFiscal, 3, "0"))) {
                throw new FazemuServiceException("Serie de Documento Fiscal inconsistente com a chave de acesso");
            }
            if (!key.nNF.equals(StringUtils.leftPad(numeroDocumentoFiscal, 9, "0"))) {
                throw new FazemuServiceException("Numero de Documento Fiscal inconsistente com a chave de acesso");
            }
            if (!key.tpEmis.equals(StringUtils.leftPad(tipoEmissao, 1, "0"))) {
                throw new FazemuServiceException("Tipo de Emissao inconsistente com a chave de acesso");
            }
            if (!key.cNF.equals(StringUtils.leftPad(codigoChaveAcesso, 8, "0"))) {
                throw new FazemuServiceException("Codigo Numerico da Chave de Acesso inconsistente com a chave de acesso");
            }
            if (!key.cDV.equals(digitoChaveAcesso)) {
                throw new FazemuServiceException("Digito Verificador da Chave de Acesso inconsistente com a chave de acesso");
            }

        } catch (org.apache.el.parser.ParseException e) {
            throw new FazemuServiceException("Chave de Acesso invalida");
        }

        // Verifica se esta na versao 4.00
        if (!"4.00".equals(versao)) {
            throw new FazemuServiceException("Versao inválida: " + versao);
        }

        // Verifica se UF Emitente esta cadastrado
        Estado estado = findEstadoByCodigoIbge(Integer.valueOf(ufEmitente));
        if (estado == null) {
            throw new FazemuServiceException("Estado do Emitente não cadastrado: " + ufEmitente);
        }

        // Verifica se CNPJ Emitente esta cadastrado
        long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(Long.valueOf(cnpjEmitente));
        EmissorRaiz emissor = findEmissorRaizById(String.valueOf(raizCnpjEmitente));
        if (emissor == null) {
            throw new FazemuServiceException("Emitente não cadastrado: " + cnpjEmitente);
        }

        //Verifica se existe certificado para o CNPJ emissor
        EmissorRaizCertificadoDigital emissorRaizCertificadoDigital = emissorRaizCertificadoDigitalRepository.findByIdEmissorRaiz(raizCnpjEmitente);
        if (emissorRaizCertificadoDigital == null || emissorRaizCertificadoDigital.getCertificadoBytes() == null) {
            throw new FazemuServiceException("Certificado Digital não cadastrado para o Emitente: " + cnpjEmitente);
        }

        DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);

        if (documentoFiscal != null) {
            // Verifica se a NFe já foi recebida e está com situacao aberta
            if (documentoFiscal.getSituacao() != null && SituacaoEnum.ABERTO.getCodigo().equals(documentoFiscal.getSituacao())) {
                throw new FazemuServiceException("NFe ja recebida e em processo de aprovacao: " + chaveAcesso);
            }

            // Não permite operacao caso nfe esteja em aguardando conciliação (CSTAT 136/468)
            if (documentoFiscal.getSituacaoAutorizador() != null && ("136".contentEquals(documentoFiscal.getSituacaoAutorizador()) || "468".contentEquals(documentoFiscal.getSituacaoAutorizador()))) {
                throw new FazemuServiceException("NFe em processo de conciliação após EPEC: " + chaveAcesso);
            }
        }

    }

    protected void persist(TNFe tNFe, Map<String, String> mapXml, Integer tipoEmissaoAtual, String chaveAcessoOriginal, ChaveAcessoNFe chaveAcessoAlterada) throws JAXBException, ParseException, org.apache.el.parser.ParseException {

        String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

        String xmlRaw = mapXml.get(MAPA_XML_RAW);
        String xmlSigned = mapXml.get(MAPA_XML_SIGNED);
        String xmlEpec = mapXml.get(MAPA_XML_EPEC);

        //Verifica existencia do documento fiscal
        DocumentoFiscal docu = documentoFiscalRepository.findByChaveAcesso(chaveAcessoOriginal);

        DocumentoFiscal documentoFiscal = new DocumentoFiscal();
        Long idDocumentoFiscal;
        if (docu == null) {
            documentoFiscal.setTipoDocumentoFiscal(TIPO_DOCUMENTO_FISCAL_NFE);
            documentoFiscal.setIdEmissor(Long.parseLong(tNFe.getInfNFe().getEmit().getCNPJ()));
            documentoFiscal.setNumeroDocumentoFiscal(Long.parseLong(tNFe.getInfNFe().getIde().getNNF()));
            documentoFiscal.setSerieDocumentoFiscal(Long.parseLong(tNFe.getInfNFe().getIde().getSerie()));
            documentoFiscal.setDataHoraEmissao(DateUtils.iso8601ToCalendar(tNFe.getInfNFe().getIde().getDhEmi()).getTime());
            Integer codigoIbge = Integer.valueOf(tNFe.getInfNFe().getIde().getCUF());
            documentoFiscal.setIdEstado(estadoRepository.findByCodigoIbge(codigoIbge).getId());
            documentoFiscal.setVersao(tNFe.getInfNFe().getVersao());
            documentoFiscal.setChaveAcesso(chaveAcessoOriginal);
            documentoFiscal.setChaveAcessoEnviada(chaveAcessoAlterada == null ? chaveAcessoOriginal : ChaveAcessoNFe.parseKey(chaveAcessoAlterada, true));
            documentoFiscal.setIdPonto(xmlEpec != null ? PontoDocumentoEnum.RECEBIDO_XML_EPEC.getCodigo() : PontoDocumentoEnum.RECEBIDO_XML_SIGNED.getCodigo());
            documentoFiscal.setSituacaoAutorizador(null);
            documentoFiscal.setUsuarioReg(usuario);
            documentoFiscal.setUsuario(usuario);
            documentoFiscal.setSituacao(SituacaoEnum.ABERTO.getCodigo());
            documentoFiscal.setAnoDocumentoFiscal(FazemuUtils.getFullYearFormat(Integer.valueOf(ChaveAcessoNFe.unparseKey(chaveAcessoOriginal).dataAAMM.substring(0, 2))));
            documentoFiscal.setIdSistema(obterCallback(tNFe));
            documentoFiscal.setTipoEmissao(null);
            documentoFiscal.setSituacaoDocumento(SituacaoDocumentoEnum.ENVIADO.getCodigo());

            String idDestinatario = null;
            if (tNFe.getInfNFe().getDest().getCNPJ() != null) {
                idDestinatario = tNFe.getInfNFe().getDest().getCNPJ();
            } else if (tNFe.getInfNFe().getDest().getCPF() != null) {
                idDestinatario = tNFe.getInfNFe().getDest().getCPF();
            } else if (tNFe.getInfNFe().getDest().getIdEstrangeiro() != null) {
                idDestinatario = tNFe.getInfNFe().getDest().getIdEstrangeiro();
            } else {
                throw new FazemuServiceException("O bloco destinatario deve informar CPF, CNPJ ou Id Estrangeiro");
            }
            documentoFiscal.setIdDestinatario(Long.parseLong(idDestinatario));

            idDocumentoFiscal = documentoFiscalRepository.insert(documentoFiscal);
        } else {
            documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(docu.getId(), null, xmlEpec != null ? PontoDocumentoEnum.RECEBIDO_XML_EPEC : PontoDocumentoEnum.RECEBIDO_XML_SIGNED, SituacaoEnum.ABERTO, null, null);
            idDocumentoFiscal = docu.getId();
            documentoFiscal = docu;
        }

        Long idDocumentoXmlRawClob = documentoClobRepository.insert(DocumentoClob.build(null, xmlRaw, usuario));
        documentoEventoRepository.insert(DocumentoEvento.build(null, idDocumentoFiscal, PontoDocumentoEnum.RECEBIDO_XML_RAW.getCodigo(), null, null, idDocumentoXmlRawClob, usuario));

        Long idDocumentoXmlSignedClob = documentoClobRepository.insert(DocumentoClob.build(null, xmlSigned, usuario));
        documentoEventoRepository.insert(DocumentoEvento.build(null, idDocumentoFiscal, PontoDocumentoEnum.RECEBIDO_XML_SIGNED.getCodigo(), null, null, idDocumentoXmlSignedClob, usuario));

        if (xmlEpec != null) {
            Long idDocumentoXmlEpecClob = documentoClobRepository.insert(DocumentoClob.build(null, xmlEpec, usuario));
            documentoEventoRepository.insert(DocumentoEvento.build(null, idDocumentoFiscal, PontoDocumentoEnum.RECEBIDO_XML_EPEC.getCodigo(), null, null, idDocumentoXmlEpecClob, usuario));

            //Altera objetos para enviar ao emissor do lote
            idDocumentoXmlSignedClob = idDocumentoXmlEpecClob;
            xmlSigned = xmlEpec;
        }

        emitirLoteService.emitirLote(ResumoDocumentoFiscal.build(idDocumentoFiscal,
                documentoFiscal.getTipoDocumentoFiscal(),
                documentoFiscal.getIdEmissor(),
                Integer.valueOf(tNFe.getInfNFe().getIde().getCUF()),
                documentoFiscal.getIdMunicipio(),
                tipoEmissaoAtual,
                documentoFiscal.getVersao(),
                idDocumentoXmlSignedClob,
                xmlSigned.length()),
                xmlEpec != null ? ServicosEnum.RECEPCAO_EVENTO_EPEC : ServicosEnum.AUTORIZACAO_NFE,
                false);

    }

    protected Document getContingenciaDocument(final TNFe tNFe, Integer tipoEmissaoAtual, ChaveAcessoNFe chaveAcessoAlterada) throws Exception {

        //Seta os valores da nova chave
        tNFe.getInfNFe().getIde().setTpEmis(String.valueOf(tipoEmissaoAtual));
        tNFe.getInfNFe().setId(String.valueOf(FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE + ChaveAcessoNFe.parseKey(chaveAcessoAlterada, true)));
        tNFe.getInfNFe().getIde().setCDV(String.valueOf(chaveAcessoAlterada.cDV));

        EstadoTipoEmissao estadoTipoEmissao = estadoTipoEmissaoRepository.findByCodigoIBGETipoEmissaoAndDataHora(Integer.valueOf(tNFe.getInfNFe().getIde().getCUF()), Integer.valueOf(tNFe.getInfNFe().getIde().getTpEmis()), DateUtils.iso8601ToCalendar(tNFe.getInfNFe().getIde().getDhEmi()).getTime());
        if (estadoTipoEmissao == null) {
            tNFe.getInfNFe().getIde().setXJust("Problema de Comunicação com SEFAZ");
            tNFe.getInfNFe().getIde().setDhCont(tNFe.getInfNFe().getIde().getDhEmi());
        } else {
            tNFe.getInfNFe().getIde().setXJust(estadoTipoEmissao.getJustificativa());
            tNFe.getInfNFe().getIde().setDhCont(DateUtils.convertDateToIsoString(estadoTipoEmissao.getDataInicio()));
        }

        Document document = XMLUtils.createNewDocument();
        context.createMarshaller().marshal(tNFe, document);

        // Retira namespace
        XMLUtils.cleanNameSpace(document);

        // Converte para String e retira espacos e quebras de linha
        String xmlRaw = XMLUtils.convertDocumentToString(document, true);
        String xmlFinal = incluiResponsavelTecnico(xmlRaw, FazemuUtils.obterRaizCNPJ(Long.valueOf(tNFe.getInfNFe().getEmit().getCNPJ())), tNFe.getInfNFe().getIde().getCUF());

        // Converte para Document
        Document docFinal = XMLUtils.convertStringToDocument(xmlFinal);
        docFinal.setXmlStandalone(true);

        return docFinal;
    }

    protected Document getEpecDocument(TNFe tNFe, Integer tipoEmissaoAtual, String chaveAcessoAlterada) throws Exception {

        TEvento.InfEvento infEvento = new TEvento.InfEvento();
        infEvento.setId("ID" + RecepcaoEventoEnum.EPEC.getCodigoEvento() + chaveAcessoAlterada + "01");
        infEvento.setCOrgao("91");
        infEvento.setTpAmb(parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, "SEFAZ_TP_AMB"));
        infEvento.setCNPJ(tNFe.getInfNFe().getEmit().getCNPJ());
        infEvento.setChNFe(chaveAcessoAlterada);
        infEvento.setDhEvento(tNFe.getInfNFe().getIde().getDhEmi());
        infEvento.setTpEvento(String.valueOf(RecepcaoEventoEnum.EPEC.getCodigoEvento()));
        infEvento.setNSeqEvento("1");
        infEvento.setVerEvento(ServicosEnum.RECEPCAO_EVENTO_EPEC.getVersao());

        TEvento.InfEvento.DetEvento detEvento = new TEvento.InfEvento.DetEvento();
        detEvento.setVersao(ServicosEnum.RECEPCAO_EVENTO_EPEC.getVersao());
        detEvento.setDescEvento("EPEC");
        detEvento.setCOrgaoAutor(tNFe.getInfNFe().getIde().getCUF());
        detEvento.setTpAutor("1");
        detEvento.setVerAplic(FazemuUtils.APLICACAO);
        detEvento.setDhEmi(tNFe.getInfNFe().getIde().getDhEmi());
        detEvento.setTpNF(tNFe.getInfNFe().getIde().getTpNF());
        detEvento.setIE(tNFe.getInfNFe().getEmit().getIE());

        TEvento.InfEvento.DetEvento.Dest destinatario = new TEvento.InfEvento.DetEvento.Dest();
        destinatario.setUF(TUf.fromValue(tNFe.getInfNFe().getDest().getEnderDest().getUF().name()));
        destinatario.setCNPJ(tNFe.getInfNFe().getDest().getCNPJ());
        destinatario.setCPF(tNFe.getInfNFe().getDest().getCPF());
        destinatario.setIE(tNFe.getInfNFe().getDest().getIE() == null || "ISENTO".equals(tNFe.getInfNFe().getDest().getIE()) ? null : tNFe.getInfNFe().getDest().getIE());
        destinatario.setVNF(tNFe.getInfNFe().getTotal().getICMSTot().getVNF());
        destinatario.setVICMS(tNFe.getInfNFe().getTotal().getICMSTot().getVICMS());
        destinatario.setVST(tNFe.getInfNFe().getTotal().getICMSTot().getVST());

        detEvento.setDest(destinatario);
        infEvento.setDetEvento(detEvento);

        TEvento evento = new TEvento();
        evento.setInfEvento(infEvento);
        evento.setVersao(ServicosEnum.RECEPCAO_EVENTO_EPEC.getVersao());

        // Marshal the Object to a Document
        Document document = XMLUtils.createNewDocument();
        contextEpec.createMarshaller().marshal(evento, document);

        // retira namespace
        XMLUtils.cleanNameSpace(document);

        XMLUtils.insertNameSpace(document);

        // Converte para String e retira espacos e quebras de linha
        String xmlEpec = XMLUtils.convertDocumentToString(document, true);

        // Converte para Document
        Document docEpec = XMLUtils.convertStringToDocument(xmlEpec);
        docEpec.setXmlStandalone(true);

        // Certificado
        long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(Long.valueOf(tNFe.getInfNFe().getEmit().getCNPJ()));
        CertificadoDigital certificado = certificadoDigitalService.getByIdEmissorRaiz(raizCnpjEmitente);

        FazemuUtils.signXml(docEpec, certificado, ServicosEnum.RECEPCAO_EVENTO_EPEC);

        return docEpec;
    }

    protected ChaveAcessoNFe getNewChaveAcesso(String chaveAcesso, Integer tipoEmissaoAtual) throws org.apache.el.parser.ParseException {
        ChaveAcessoNFe key = ChaveAcessoNFe.unparseKey(FazemuUtils.normalizarChaveAcesso(chaveAcesso));

        // Chave com novo tipo de emissao
        return new ChaveAcessoNFe(key.cUF, key.dataAAMM, key.cnpjCpf, key.mod, key.serie, key.nNF, String.valueOf(tipoEmissaoAtual), key.cNF);
    }

    protected String obterCallback(TNFe nfe) {
        try {
            String obterCallback = null;

            List<ObsCont> obsContList = nfe.getInfNFe().getInfAdic().getObsCont();
            for (ObsCont obsCont : obsContList) {
                if ("Callback".equalsIgnoreCase(obsCont.getXCampo())) {
                    obterCallback = obsCont.getXTexto();
                    break; // campo de informacoes adicionais com o sistema para Callback encontrado; encerra o loop
                }
            }

            return obterCallback;

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    protected String incluiResponsavelTecnico(String xml, Long raizCnpjEmitente, String codigoIbge) {
        //Verifica se estado utiliza responsavel tecnico
        EstadoConfiguracao ec = estadoConfiguracaoRepository.findByTipoDocumentoFiscalAndSiglaIbge(TIPO_DOCUMENTO_FISCAL_NFE, Long.valueOf(codigoIbge));
        if (!"S".equals(ec.getInResponsavelTecnico())) {
            return xml;
        }

        //Verifica se existe responsavel tecnico cadastrado
        ResponsavelTecnico responsavelTecnico = responsavelTecnicoRepository.findByIdEmissorRaiz(raizCnpjEmitente);
        if (responsavelTecnico == null) {
            return xml;
        }

        String cnpj = StringUtils.leftPad(String.valueOf(responsavelTecnico.getCnpj()).replaceAll("\\D", ""), 14, "0");
        StringBuilder sb = new StringBuilder()
                .append("<infRespTec>")
                .append("<CNPJ>").append(cnpj).append("</CNPJ>")
                .append("<xContato>").append(responsavelTecnico.getContato().trim()).append("</xContato>")
                .append("<email>").append(responsavelTecnico.getEmail().trim()).append("</email>")
                .append("<fone>").append(responsavelTecnico.getTelefone()).append("</fone>")
                .append("</infRespTec></infNFe></NFe>");

        return xml.replace("</infNFe></NFe>", sb.toString());
    }

}
