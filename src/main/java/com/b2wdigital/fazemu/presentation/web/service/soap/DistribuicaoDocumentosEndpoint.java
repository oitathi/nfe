package com.b2wdigital.fazemu.presentation.web.service.soap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import com.b2wdigital.fazemu.business.service.DistribuicaoDocumentosService;
import com.newrelic.api.agent.Trace;

import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresse.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresseResponse;

/**
 * Distribuicao Documentos Endpoint.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Endpoint
public class DistribuicaoDocumentosEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistribuicaoDocumentosEndpoint.class);

    @Autowired
    private DistribuicaoDocumentosService distribuicaoDocumentosService;

    @Trace(dispatcher = true)
    @PayloadRoot(namespace = "http://www.portalfiscal.inf.br/nfe/wsdl/NFeDistribuicaoDFe", localPart = "nfeDadosMsg")
    @ResponsePayload
    public NfeDistDFeInteresseResponse processWS(@RequestPayload NfeDadosMsg request) {
        LOGGER.debug("DistribuicaoDocumentosEndpoint: processWS");

        try {
            return distribuicaoDocumentosService.process(request, 1);
        } catch (Exception e) {
            LOGGER.error("Erro ao fazer a distribuicao documentos eletronicos", e);
            e.printStackTrace();
            return null;
        }
    }


}
