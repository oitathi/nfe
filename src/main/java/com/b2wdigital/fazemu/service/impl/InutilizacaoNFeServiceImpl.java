package com.b2wdigital.fazemu.service.impl;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

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
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.business.service.EmitirLoteService;
import com.b2wdigital.fazemu.business.service.InutilizacaoNFeService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.EmissorRaiz;
import com.b2wdigital.fazemu.domain.EmissorRaizCertificadoDigital;
import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.domain.ResumoDocumentoFiscal;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.inutNFe_v4.TInutNFe;
import com.b2wdigital.nfe.schema.v4v160b.inutNFe_v4.TInutNFe.InfInut;
import com.b2wdigital.nfe.schema.v4v160b.inutNFe_v4.TRetInutNFe;
import com.fasterxml.jackson.databind.util.ISO8601Utils;

import br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeResultMsg;

/**
 * InutilizacaoNFe Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@SuppressWarnings("deprecation")
@Service
public class InutilizacaoNFeServiceImpl extends AbstractNFeServiceImpl implements InutilizacaoNFeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InutilizacaoNFeServiceImpl.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private EmissorRaizCertificadoDigitalRepository emissorRaizCertificadoDigitalRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private DocumentoEventoRepository documentoEventoRepository;

    @Autowired
    private EmitirLoteService emitirLoteService;

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Autowired
    @Qualifier("nfeInutilizacaoContext")
    private JAXBContext context;

    /**
     * process
     *
     * @param request
     * @param usuario
     * @return
     * @throws Exception
     */
    @Override
    public NfeResultMsg process(NfeDadosMsg request, String usuario) throws Exception {
        TInutNFe inutNFe = null;
        try {
            LOGGER.debug("InutilizacaoNFeServiceImpl: process");

            String statusFazemu = (String) redisOperationsService.getKeyValue(FazemuUtils.FAZEMU_NFE_STATUS);
            if (!"ON".equals(statusFazemu)) {
                throw new FazemuServiceException("Sistema Indisponível no momento.");
            }

            // Obtem documento e callback se houver
            Map<String, Document> map = getDocument(request);
            Document docInutNFe = map.get("xml");

            // retira namespace
            XMLUtils.cleanNameSpace(docInutNFe);

            // Converte para String e retira espacos e quebras de linha
            String xmlFinal = XMLUtils.convertDocumentToString(docInutNFe);

            // Converte para Document
            Document docFinal = XMLUtils.convertStringToDocument(xmlFinal);
            docFinal.setXmlStandalone(true);

            inutNFe = (TInutNFe) context.createUnmarshaller().unmarshal(docInutNFe);
            preValidations(inutNFe);

            //Determina o tipo de emissao
            Integer tipoEmissaoAtual = getTipoEmissao(inutNFe.getInfInut().getCUF());

            // Certificado
            long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(Long.valueOf(inutNFe.getInfInut().getCNPJ()));
            CertificadoDigital certificado = certificadoDigitalService.getByIdEmissorRaiz(raizCnpjEmitente);

            FazemuUtils.signXml(docFinal, certificado, ServicosEnum.INUTILIZACAO);

            Document docCallback = map.get("callback");

            persist(inutNFe, tipoEmissaoAtual, XMLUtils.convertDocumentToString(docFinal), docCallback, usuario);

            return createResultMessage(inutNFe, null, FazemuUtils.PREFIX + FazemuUtils.CODE_006_INUTILIZACAO_ACCEPTED, FazemuUtils.MSG_006_INUTILIZACAO_ACCEPTED);

        } catch (Exception e) {
            return createResultMessage(inutNFe, e, null, null);
        }
    }

    protected Map<String, Document> getDocument(NfeDadosMsg nfeDadosMsg) throws JAXBException, ParserConfigurationException {

        Map<String, Document> map = new HashMap<>();

        Document document = XMLUtils.createNewDocument();
        context.createMarshaller().marshal(nfeDadosMsg, document);

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
        } catch (org.w3c.dom.DOMException e) {
            // DO NOTHING
        }

        return map;
    }

    protected void preValidations(TInutNFe inutNFe) {

        String ufEmitente = inutNFe.getInfInut().getCUF();
        String anoDocumentoFiscal = inutNFe.getInfInut().getAno();
        String cnpjEmitente = inutNFe.getInfInut().getCNPJ();
        String modeloDocumentoFiscal = inutNFe.getInfInut().getMod();
        String serieDocumentoFiscal = inutNFe.getInfInut().getSerie();
        String numeroDocumentoFiscalInicial = inutNFe.getInfInut().getNNFIni();
        String numeroDocumentoFiscalFinal = inutNFe.getInfInut().getNNFFin();
        String justificativa = inutNFe.getInfInut().getXJust();

        // Verifica informacoes obrigatorias
        if (StringUtils.isBlank(ufEmitente)) {
            throw new FazemuServiceException("UF não encontrado no XML");
        }
        if (StringUtils.isBlank(anoDocumentoFiscal)) {
            throw new FazemuServiceException("Ano de Documento Fiscal Inicial  não encontrado no XML");
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
        if (StringUtils.isBlank(numeroDocumentoFiscalInicial)) {
            throw new FazemuServiceException("Numero de Documento Fiscal Inicial não encontrado no XML");
        }
        if (StringUtils.isBlank(numeroDocumentoFiscalFinal)) {
            throw new FazemuServiceException("Numero de Documento Fiscal Final não encontrado no XML");
        }
        if (StringUtils.isBlank(justificativa)) {
            throw new FazemuServiceException("Justificativa não encontrada no XML");
        }

        // Inutilizacao implementada PONTUAL DE NUMERO (apenas 1 nota por vez)
        if (!numeroDocumentoFiscalInicial.equals(numeroDocumentoFiscalFinal)) {
            throw new FazemuServiceException("Inutilizacao deve ter o mesmo numero inicial e final de NFe");
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

        // Verifica se a NFe já foi recebida e está com situacao aberta
        DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByDadosDocumentoFiscal(TIPO_DOCUMENTO_FISCAL_NFE, Long.valueOf(cnpjEmitente), Long.valueOf(numeroDocumentoFiscalInicial), Long.valueOf(serieDocumentoFiscal), FazemuUtils.getFullYearFormat(Integer.valueOf(anoDocumentoFiscal)), estado.getId());
        if (documentoFiscal != null && SituacaoEnum.ABERTO.getCodigo().equals(documentoFiscal.getSituacao())) {
            throw new FazemuServiceException("NFe ja recebida e em situacao aberta: " + documentoFiscal.getId());
        }

        // Não permite operacao caso nfe esteja em aguardando conciliação (CSTAT 468)
        if (documentoFiscal != null && "468".contentEquals(documentoFiscal.getSituacaoAutorizador())) {
            throw new FazemuServiceException("NFe em processo de conciliação após EPEC: " + documentoFiscal.getChaveAcesso());
        }

    }

    protected void persist(TInutNFe inutNFe, Integer tipoEmissaoAtual, String xml, Document docCallback, String usuario) throws JAXBException, ParseException, org.apache.el.parser.ParseException {

        if (usuario == null) {
            usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);
        }

        // Verificar se existe callback
        String sistemaWs = null;
        if (docCallback != null) {
            sistemaWs = docCallback.getFirstChild().getTextContent();
        }

        //Verifica existencia do documento fiscal
        Long idEmissor = Long.valueOf(inutNFe.getInfInut().getCNPJ());
        Estado estado = estadoRepository.findByCodigoIbge(Integer.valueOf(inutNFe.getInfInut().getCUF()));
        Integer anoDocumentoFiscal = FazemuUtils.getFullYearFormat(Integer.valueOf(inutNFe.getInfInut().getAno()));
        Long numeroDocumentoFiscal = Long.valueOf(inutNFe.getInfInut().getNNFIni());
        Long serieDocumentoFiscal = Long.valueOf(inutNFe.getInfInut().getSerie());
        DocumentoFiscal docu = documentoFiscalRepository.findByDadosDocumentoFiscal(TIPO_DOCUMENTO_FISCAL_NFE, idEmissor, numeroDocumentoFiscal, serieDocumentoFiscal, anoDocumentoFiscal, estado.getId());

        DocumentoFiscal documentoFiscal = new DocumentoFiscal();
        Long idDocumentoFiscal;
        if (docu == null) {
            documentoFiscal.setTipoDocumentoFiscal(TIPO_DOCUMENTO_FISCAL_NFE);
            documentoFiscal.setIdEmissor(idEmissor);
            documentoFiscal.setNumeroDocumentoFiscal(numeroDocumentoFiscal);
            documentoFiscal.setSerieDocumentoFiscal(serieDocumentoFiscal);
            documentoFiscal.setDataHoraEmissao(null);
            Integer codigoIbge = Integer.valueOf(inutNFe.getInfInut().getCUF());
            documentoFiscal.setIdEstado(estadoRepository.findByCodigoIbge(codigoIbge).getId());
            documentoFiscal.setVersao(inutNFe.getVersao());
            documentoFiscal.setChaveAcesso(null);
            documentoFiscal.setChaveAcessoEnviada(null);
            documentoFiscal.setIdPonto(PontoDocumentoEnum.INUTILIZACAO.getCodigo());
            documentoFiscal.setSituacaoAutorizador(null);
            documentoFiscal.setUsuarioReg(usuario);
            documentoFiscal.setUsuario(usuario);
            documentoFiscal.setSituacao(SituacaoEnum.ABERTO.getCodigo());
            documentoFiscal.setAnoDocumentoFiscal(anoDocumentoFiscal);
            documentoFiscal.setIdSistema(sistemaWs != null ? sistemaWs : StringUtils.EMPTY);
            documentoFiscal.setSituacaoDocumento(SituacaoDocumentoEnum.ENVIADO.getCodigo());

            idDocumentoFiscal = documentoFiscalRepository.insert(documentoFiscal);
        } else {
            documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(docu.getId(), null, PontoDocumentoEnum.INUTILIZACAO, SituacaoEnum.ABERTO, null, null);
            idDocumentoFiscal = docu.getId();
            documentoFiscal = docu;
        }

        Long idDocumentoXmlClob = documentoClobRepository.insert(DocumentoClob.build(null, xml, usuario));
        documentoEventoRepository.insert(DocumentoEvento.build(null, idDocumentoFiscal, PontoDocumentoEnum.INUTILIZACAO.getCodigo(), null, null, idDocumentoXmlClob, usuario));

        emitirLoteService.emitirLote(ResumoDocumentoFiscal.build(idDocumentoFiscal,
                documentoFiscal.getTipoDocumentoFiscal(),
                documentoFiscal.getIdEmissor(),
                Integer.valueOf(inutNFe.getInfInut().getCUF()),
                documentoFiscal.getIdMunicipio(),
                tipoEmissaoAtual,
                documentoFiscal.getVersao(),
                idDocumentoXmlClob,
                xml.length()),
                ServicosEnum.INUTILIZACAO,
                false);

    }

    protected NfeResultMsg createResultMessage(TInutNFe inutNFe, Exception exception, String code, String message) throws Exception {
        InfInut reqInfInut = inutNFe.getInfInut();

        TRetInutNFe.InfInut retInfInut = new TRetInutNFe.InfInut();
        retInfInut.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
        retInfInut.setVerAplic(FazemuUtils.APLICACAO);
        retInfInut.setCStat(code);
        retInfInut.setXMotivo(message == null ? exception.getMessage() : message);
        retInfInut.setCUF(reqInfInut.getCUF());
        retInfInut.setAno(reqInfInut.getAno());
        retInfInut.setCNPJ(reqInfInut.getCNPJ());
        retInfInut.setMod(reqInfInut.getMod());
        retInfInut.setSerie(reqInfInut.getSerie());
        retInfInut.setNNFIni(reqInfInut.getNNFIni());
        retInfInut.setNNFFin(reqInfInut.getNNFFin());
        retInfInut.setDhRecbto(ISO8601Utils.format(new Date(), false, TimeZone.getDefault(), FazemuUtils.LOCALE));
        retInfInut.setNProt(null);

        TRetInutNFe retorno = new TRetInutNFe();
        retorno.setVersao(ServicosEnum.INUTILIZACAO.getNome());
        retorno.setInfInut(retInfInut);

        Document document = null;
        try {
            // Create the Document
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            document = db.newDocument();

            // Marshal the Object to a Document
            context.createMarshaller().marshal(retorno, document);
        } catch (JAXBException e) {
            throw new Exception(e.getMessage(), e.getCause());
        } catch (ParserConfigurationException e) {
            throw new Exception(e.getMessage(), e.getCause());
        }

        NfeResultMsg nfeResultMsg = new NfeResultMsg();
        nfeResultMsg.getContent().add(document.getDocumentElement());

        return nfeResultMsg;
    }

}
