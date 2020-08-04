package com.b2wdigital.fazemu.integration.client.soap.sefaz;

import static com.b2wdigital.fazemu.enumeration.ServicosEnum.RETORNO_AUTORIZACAO_NFE;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.ws.BindingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.b2wdigital.fazemu.business.client.ConsultarReciboNFeClient;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.domain.CertificadoDigitalRedis;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.exception.FazemuClientException;
import com.b2wdigital.fazemu.net.CustomCertSSLSocketFactory;
import com.b2wdigital.fazemu.net.NFeSOAPHeaderHandler;
import com.b2wdigital.fazemu.utils.FazemuUtils;

import br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NFeRetAutorizacao4;
import br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NFeRetAutorizacao4Soap12;
import br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NfeResultMsg;

@Repository
public class ConsultarReciboNFeSefazSoapClient implements ConsultarReciboNFeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarReciboNFeSefazSoapClient.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Override
    public NfeResultMsg nfeRetAutorizacaoLote(String url, NfeDadosMsg nfeDadosMsg, ResumoLote lote) throws Exception {
        try {
            LOGGER.debug("ConsultarReciboNFeSefazSoapClient: nfeRetAutorizacaoLote");

            long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(lote.getIdEmissor());
            CertificadoDigitalRedis certificadoDigitalRedis = certificadoDigitalService.getInstance(raizCnpjEmitente);

            ByteArrayInputStream truststoreInputStream = new ByteArrayInputStream(parametrosInfraRepository.getAsByteArray(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIB_TRUSTSTORE));
            char[] truststorePassword = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TRUSTSTORE_PASSWORD).toCharArray();

            SSLSocketFactory sslSocketFactory = CustomCertSSLSocketFactory.getInstance(new ByteArrayInputStream(certificadoDigitalRedis.getCertificadoDigitalByte()),
                    new String(Base64.getDecoder().decode(certificadoDigitalRedis.getSenha())).toCharArray(),
                    truststoreInputStream, truststorePassword);

            NFeRetAutorizacao4 service = new NFeRetAutorizacao4();
            NFeRetAutorizacao4Soap12 port = service.getNFeRetAutorizacao4Soap12();

            BindingProvider bindingProvider = (BindingProvider) port;
            bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
            bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", sslSocketFactory);
            bindingProvider.getRequestContext().put("weblogic.wsee.jaxws.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
            bindingProvider.getRequestContext().put("com.sun.xml.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
            bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
            NFeSOAPHeaderHandler.prepareHeader(bindingProvider, RETORNO_AUTORIZACAO_NFE.getNamespace(), lote.getIdEmissor().intValue(), lote.getVersao());

            return port.nfeRetAutorizacaoLote(nfeDadosMsg);
        } catch (Exception e) {
            throw new FazemuClientException(e);
        }
    }

}
