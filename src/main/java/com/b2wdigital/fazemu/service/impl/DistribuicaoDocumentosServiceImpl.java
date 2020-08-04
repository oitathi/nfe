package com.b2wdigital.fazemu.service.impl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.DistribuicaoDocumentosService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.integration.client.soap.sefaz.DistribuicaoDocumentosSefazSoapClient;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.distDFeInt_v1.DistDFeInt;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.retDistDFeInt_v1.RetDistDFeInt;

import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresse;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresseResponse;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresseResponse.NfeDistDFeInteresseResult;

/**
 * Distribuicao Documentos Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class DistribuicaoDocumentosServiceImpl extends AbstractNFeServiceImpl implements DistribuicaoDocumentosService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistribuicaoDocumentosServiceImpl.class);

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private DistribuicaoDocumentosSefazSoapClient distribuicaoDocumentosSefazSoapClient;

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Autowired
    @Qualifier("nfeDistribuicaoDocumentosContext")
    private JAXBContext contextDistribuicao;

    @Override
    public NfeDistDFeInteresseResponse process(NfeDistDFeInteresse.NfeDadosMsg request, int index) throws Exception {

        try {
            LOGGER.debug("DistribuicaoDocumentosService: process");

            String statusFazemu = (String) redisOperationsService.getKeyValue(FazemuUtils.FAZEMU_NFE_STATUS);
            if (!"ON".equals(statusFazemu)) {
                throw new FazemuServiceException("Sistema Indisponível no momento.");
            }

            //Pega documento pela raiz
            Document docDistDFeInt = getDocument(request, index);

            // Convert to String and unpretty xml
            String xmlFinal = XMLUtils.convertDocumentToString(docDistDFeInt);

            // Convert to Document
            Document docFinal = XMLUtils.convertStringToDocument(xmlFinal);
            docFinal.setXmlStandalone(true);

            //Clean document namespace
            XMLUtils.cleanNameSpace(docFinal);

            DistDFeInt distDFeInt = (DistDFeInt) contextDistribuicao.createUnmarshaller().unmarshal(docDistDFeInt);

            //Determina o tipo de emissao
            Integer tipoEmissaoAtual = getTipoEmissao(distDFeInt.getCUFAutor());

            NfeDistDFeInteresse.NfeDadosMsg nfeDadosMsg = new NfeDistDFeInteresse.NfeDadosMsg();
            nfeDadosMsg.getContent().add(docFinal.getDocumentElement());

            String url = getUrl(91, tipoEmissaoAtual, ServicosEnum.DISTRIBUICAO_DFE.getNome(), distDFeInt.getVersao()); //TODO transformar 91 em parametro

            return distribuicaoDocumentosSefazSoapClient.nfeDistribuicaoDocumentos(url, nfeDadosMsg, Integer.valueOf(distDFeInt.getCUFAutor()), Long.valueOf(distDFeInt.getCNPJ()));

        } catch (Exception e) {
            return createResultMessage(FazemuUtils.PREFIX + FazemuUtils.ERROR_CODE, e.getMessage());
        }
    }

    protected Document getDocument(NfeDistDFeInteresse.NfeDadosMsg nfeDadosMsg, int index) throws JAXBException, ParserConfigurationException {

        NfeDistDFeInteresse nfeDistDFeInteresse = new NfeDistDFeInteresse();
        nfeDistDFeInteresse.setNfeDadosMsg(nfeDadosMsg);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        Marshaller marshaller = contextDistribuicao.createMarshaller();
        marshaller.marshal(nfeDistDFeInteresse, document);

        //delete nfeDistDFeInteresse
        Node node = document.getFirstChild().getChildNodes().item(0);
        factory.setNamespaceAware(true);
        Document newDocument = builder.newDocument();
        Node importedNode = newDocument.importNode(node, true);
        newDocument.appendChild(importedNode);

        //delete nfeDadosMsg
        Document chilDocument = getChildDocument(newDocument, index);

        //delete nfeDadosMsg2
        return chilDocument;
    }

    protected Document getChildDocument(Document document, int index) throws JAXBException, ParserConfigurationException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        Node node = document.getFirstChild().getChildNodes().item(index);
        factory.setNamespaceAware(true);
        Document newDocument = builder.newDocument();
        Node importedNode = newDocument.importNode(node, true);
        newDocument.appendChild(importedNode);

        return newDocument;
    }

    protected NfeDistDFeInteresseResponse createResultMessage(String code, String message) throws Exception {

        RetDistDFeInt retorno = new RetDistDFeInt();
        retorno.setVersao(ServicosEnum.DISTRIBUICAO_DFE.getVersao());
        retorno.setTpAmb(parametrosInfraRepository.getAsString(FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase(), ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
        retorno.setVerAplic(FazemuUtils.APLICACAO);
        retorno.setCStat(code);
        retorno.setXMotivo(message);
        retorno.setDhResp(null);
        retorno.setUltNSU(null);
        retorno.setMaxNSU(null);

        Document document = null;
        try {
            // Create the Document
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            document = db.newDocument();

            // Marshal the Object to a Document
            Marshaller marshaller = contextDistribuicao.createMarshaller();
            marshaller.marshal(retorno, document);
        } catch (JAXBException e) {
            throw new Exception(e.getMessage(), e.getCause());
        } catch (ParserConfigurationException e) {
            throw new Exception(e.getMessage(), e.getCause());
        }

        NfeDistDFeInteresseResult result = new NfeDistDFeInteresseResult();
        result.getContent().add(document.getDocumentElement());

        NfeDistDFeInteresseResponse response = new NfeDistDFeInteresseResponse();
        response.setNfeDistDFeInteresseResult(result);

        return response;
    }

}
