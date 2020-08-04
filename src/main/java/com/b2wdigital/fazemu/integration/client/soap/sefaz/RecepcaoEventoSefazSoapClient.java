package com.b2wdigital.fazemu.integration.client.soap.sefaz;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.ws.BindingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.b2wdigital.fazemu.business.client.RecepcaoEventoClient;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.domain.CertificadoDigitalRedis;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.exception.FazemuClientException;
import com.b2wdigital.fazemu.net.CustomCertSSLSocketFactory;
import com.b2wdigital.fazemu.net.NFeSOAPHeaderHandler;
import com.b2wdigital.fazemu.utils.FazemuUtils;

import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NFeRecepcaoEvento4;
import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NFeRecepcaoEvento4Soap12;
import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg;

@Component
public class RecepcaoEventoSefazSoapClient implements RecepcaoEventoClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecepcaoEventoSefazSoapClient.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    public NfeResultMsg nfeRecepcaoEvento(String url, NfeDadosMsg nfeDadosMsg, ResumoLote lote, ServicosEnum servico) throws Exception {
        try {
            LOGGER.debug("RecepcaoEventoSefazSoapClient: nfeRecepcaoEvento");

            long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(lote.getIdEmissor());
            CertificadoDigitalRedis certificadoDigitalRedis = certificadoDigitalService.getInstance(raizCnpjEmitente);

            ByteArrayInputStream truststoreInputStream = new ByteArrayInputStream(parametrosInfraRepository.getAsByteArray(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIB_TRUSTSTORE));
            char[] truststorePassword = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TRUSTSTORE_PASSWORD).toCharArray();

            SSLSocketFactory sslSocketFactory = CustomCertSSLSocketFactory.getInstance(new ByteArrayInputStream(certificadoDigitalRedis.getCertificadoDigitalByte()),
                    new String(Base64.getDecoder().decode(certificadoDigitalRedis.getSenha())).toCharArray(),
                    truststoreInputStream, truststorePassword);

            NFeRecepcaoEvento4 service = new NFeRecepcaoEvento4();
            NFeRecepcaoEvento4Soap12 port = service.getNFeRecepcaoEvento4Soap12();

            BindingProvider bindingProvider = (BindingProvider) port;
            bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
            bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", sslSocketFactory);
            bindingProvider.getRequestContext().put("weblogic.wsee.jaxws.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
            bindingProvider.getRequestContext().put("com.sun.xml.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
            bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
            NFeSOAPHeaderHandler.prepareHeader(bindingProvider, servico.getNamespace(), lote.getUf(), lote.getVersao());

            return port.nfeRecepcaoEvento(nfeDadosMsg);
        } catch (Exception e) {
            throw new FazemuClientException(e);
        }

    }

}
