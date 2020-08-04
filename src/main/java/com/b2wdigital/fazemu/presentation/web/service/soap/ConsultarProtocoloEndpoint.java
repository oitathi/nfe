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

import br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeResultMsg;

import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.ConsultarProtocoloService;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TRetConsSitNFe;
import com.newrelic.api.agent.Trace;

/**
 * Consultar Protocolo Endpoint.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Endpoint
public class ConsultarProtocoloEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarProtocoloEndpoint.class);

    @Autowired
    private ConsultarProtocoloService consultarProtocoloService;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    @Qualifier("nfeConsultarProtocoloContext")
    private JAXBContext context;

    @Trace(dispatcher = true)
    @PayloadRoot(namespace = "http://www.portalfiscal.inf.br/nfe/wsdl/NFeConsultaProtocolo4", localPart = "nfeDadosMsg")
    @ResponsePayload
    public NfeResultMsg processWS(@RequestPayload NfeDadosMsg request) throws ParserConfigurationException, JAXBException {
        LOGGER.debug("ConsultarProtocoloService: processWS");

        try {
            return consultarProtocoloService.process(request);
        } catch (Exception e) {
            LOGGER.error("Erro ao fazer a consulta de protocolo", e);
            e.printStackTrace();
            return createResultMessage(FazemuUtils.PREFIX + FazemuUtils.ERROR_CODE, e.getMessage());
        }
    }

    private NfeResultMsg createResultMessage(String code, String message) throws ParserConfigurationException, JAXBException {

        TRetConsSitNFe tRetConsSitNFe = new TRetConsSitNFe();
        tRetConsSitNFe.setVersao(ServicosEnum.CONSULTA_PROTOCOLO.getVersao());
        tRetConsSitNFe.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
        tRetConsSitNFe.setVerAplic(FazemuUtils.APLICACAO);
        tRetConsSitNFe.setCStat(code);
        tRetConsSitNFe.setXMotivo(message);
        tRetConsSitNFe.setCUF(null);
        tRetConsSitNFe.setDhRecbto(null);
        tRetConsSitNFe.setChNFe(null);
        tRetConsSitNFe.setProtNFe(null);
        tRetConsSitNFe.setRetCancNFe(null);
        tRetConsSitNFe.setProtNFe(null);

        Document document = XMLUtils.createNewDocument();
        context.createMarshaller().marshal(tRetConsSitNFe, document);

        NfeResultMsg nfeResultMsg = new NfeResultMsg();
        nfeResultMsg.getContent().add(document.getDocumentElement());

        return nfeResultMsg;
    }

}
