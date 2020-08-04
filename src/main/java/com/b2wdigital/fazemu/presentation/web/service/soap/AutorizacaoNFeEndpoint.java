package com.b2wdigital.fazemu.presentation.web.service.soap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.w3c.dom.Document;

import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.AutorizacaoNFeService;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TRetEnviNFe;
import com.newrelic.api.agent.Trace;

import br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeResultMsg;

/**
 * AutorizacaoNFe Endpoint.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Endpoint
public class AutorizacaoNFeEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutorizacaoNFeEndpoint.class);

    @Autowired
    private AutorizacaoNFeService autorizacaoNFeService;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    @Qualifier("nfeAutorizacaoContext")
    private JAXBContext contextAutorizacao;

    @Trace(dispatcher = true)
    @PayloadRoot(namespace = "http://www.portalfiscal.inf.br/nfe/wsdl/NFeAutorizacao4", localPart = "nfeDadosMsg")
    @ResponsePayload
    public NfeResultMsg processWS(@RequestPayload NfeDadosMsg request) throws Exception {
        LOGGER.debug("AutorizacaoNFeEndpoint: processWS");

        try {
            autorizacaoNFeService.process(request);
            return createResultMessage(FazemuUtils.PREFIX + FazemuUtils.CODE_001_NFE_ACCEPTED, FazemuUtils.MSG_001_NFE_ACCEPTED);

        } catch (Exception e) {
            LOGGER.error("Erro ao fazer a autorizacao de nfe", e);
            e.printStackTrace();
            return createResultMessage(FazemuUtils.PREFIX + FazemuUtils.ERROR_CODE, e.getMessage());
        }
    }

    private NfeResultMsg createResultMessage(String code, String message) throws Exception {

        TRetEnviNFe retorno = new TRetEnviNFe();
        retorno.setVersao(ServicosEnum.AUTORIZACAO_NFE.getVersao());
        retorno.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
        retorno.setVerAplic(FazemuUtils.APLICACAO);
        retorno.setCStat(code);
        retorno.setXMotivo(message);
        retorno.setCUF(null);
        retorno.setDhRecbto(null);
        retorno.setInfRec(null);
        retorno.setProtNFe(null);

        Document document = null;
        try {
            // Marshal the Object to a Document
            document = XMLUtils.createNewDocument();
            contextAutorizacao.createMarshaller().marshal(retorno, document);
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
