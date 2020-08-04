package com.b2wdigital.fazemu.presentation.web.service.soap;

import br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NfeResultMsg;
import com.b2wdigital.fazemu.business.service.ConsultarStatusServicoService;
import com.newrelic.api.agent.Trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/**
 * Consultar Status Servico Endpoint.
 * 
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Endpoint
public class ConsultarStatusServicoEndpoint {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarStatusServicoEndpoint.class);

    @Autowired
    private ConsultarStatusServicoService consultarStatusServicoService;

    @Trace(dispatcher = true)
    @PayloadRoot(namespace = "http://www.portalfiscal.inf.br/nfe/wsdl/NFeStatusServico4", localPart = "nfeDadosMsg")
    @ResponsePayload
    public NfeResultMsg processWS(@RequestPayload NfeDadosMsg request) {
        try {
            LOGGER.debug("ConsultaStatusServicoEndpoint: processWS");
            return consultarStatusServicoService.process(request, null);
        } catch (Exception e) {
            LOGGER.error("Erro ao fazer a consulta status servico", e);
            return null;
        }
    }

}
