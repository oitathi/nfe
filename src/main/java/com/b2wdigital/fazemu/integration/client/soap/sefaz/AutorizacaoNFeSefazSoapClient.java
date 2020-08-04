package com.b2wdigital.fazemu.integration.client.soap.sefaz;

import static com.b2wdigital.fazemu.enumeration.ServicosEnum.AUTORIZACAO_NFE;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.ws.BindingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.b2wdigital.fazemu.business.client.AutorizacaoNFeClient;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.domain.CertificadoDigitalRedis;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.exception.FazemuClientException;
import com.b2wdigital.fazemu.net.CustomCertSSLSocketFactory;
import com.b2wdigital.fazemu.net.NFeSOAPHeaderHandler;
import com.b2wdigital.fazemu.utils.FazemuUtils;

import br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NFeAutorizacao4;
import br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NFeAutorizacao4Soap12;
import br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeResultMsg;

@Repository
public class AutorizacaoNFeSefazSoapClient implements AutorizacaoNFeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutorizacaoNFeSefazSoapClient.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Override
    public NfeResultMsg nfeAutorizacaoLote(String url, NfeDadosMsg nfeDadosMsg, ResumoLote lote) throws Exception {
        try {
            LOGGER.info("AutorizacaoNFeSefazSoapClient: nfeAutorizacaoLote");

            long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(lote.getIdEmissor());
            CertificadoDigitalRedis certificadoDigitalRedis = certificadoDigitalService.getInstance(raizCnpjEmitente);

            ByteArrayInputStream truststoreInputStream = new ByteArrayInputStream(parametrosInfraRepository.getAsByteArray(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIB_TRUSTSTORE));
            char[] truststorePassword = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TRUSTSTORE_PASSWORD).toCharArray();

            SSLSocketFactory sslSocketFactory = CustomCertSSLSocketFactory.getInstance(new ByteArrayInputStream(certificadoDigitalRedis.getCertificadoDigitalByte()),
                    new String(Base64.getDecoder().decode(certificadoDigitalRedis.getSenha())).toCharArray(),
                    truststoreInputStream, truststorePassword);

            NFeAutorizacao4 service = new NFeAutorizacao4();
            NFeAutorizacao4Soap12 port = service.getNFeAutorizacao4Soap12();

            BindingProvider bindingProvider = (BindingProvider) port;
            bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
            bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", sslSocketFactory);
            bindingProvider.getRequestContext().put("weblogic.wsee.jaxws.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
            bindingProvider.getRequestContext().put("com.sun.xml.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
            bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
            NFeSOAPHeaderHandler.prepareHeader(bindingProvider, AUTORIZACAO_NFE.getNamespace(), lote.getUf(), lote.getVersao());

            return port.nfeAutorizacaoLote(nfeDadosMsg);
        } catch (Exception e) {
            throw new FazemuClientException(e);
        }
    }

}
