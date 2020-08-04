package com.b2wdigital.fazemu.presentation.web.service.soap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import com.b2wdigital.fazemu.business.service.RecepcaoEventoService;
import com.newrelic.api.agent.Trace;

import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg;

/**
 * Recepcao de Evento Endpoint.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Endpoint
public class RecepcaoEventoEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecepcaoEventoEndpoint.class);

    @Autowired
    private RecepcaoEventoService recepcaoEventoService;

    @Trace(dispatcher = true)
    @PayloadRoot(namespace = "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4", localPart = "nfeDadosMsg")
    @ResponsePayload
    public NfeResultMsg processWS(@RequestPayload NfeDadosMsg request) {
        LOGGER.debug("RecepcaoEventoEndpoint: processWS");

        try {
            String usuario = null;
            return recepcaoEventoService.process(request, usuario);
        } catch (Exception e) {
            LOGGER.error("Erro ao fazer a recepcao evento de nfe", e);
            e.printStackTrace();
            return null;
        }
    }

}
