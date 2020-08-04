package com.b2wdigital.fazemu.presentation.web.service.soap;

import br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeResultMsg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import com.b2wdigital.fazemu.business.service.InutilizacaoNFeService;
import com.newrelic.api.agent.Trace;

/**
 * Inutilizacao NFe Endpoint.
 * 
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Endpoint
public class InutilizacaoNFeEndpoint {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(InutilizacaoNFeEndpoint.class);

    @Autowired
    private InutilizacaoNFeService inutilizacaoNFeService;

    @Trace(dispatcher = true)
    @PayloadRoot(namespace = "http://www.portalfiscal.inf.br/nfe/wsdl/NFeInutilizacao4", localPart = "nfeDadosMsg")
    @ResponsePayload
    public NfeResultMsg processWS(@RequestPayload NfeDadosMsg request) {
        try {
            LOGGER.debug("processWS");
            String usuario = null;
            return inutilizacaoNFeService.process(request, usuario);
        } catch (Exception e) {
            LOGGER.error("Erro ao fazer a inutilizacao de nfe", e);
            return null;
        }
    }

}
