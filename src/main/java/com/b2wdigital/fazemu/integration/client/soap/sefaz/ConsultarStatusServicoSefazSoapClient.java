package com.b2wdigital.fazemu.integration.client.soap.sefaz;

import static com.b2wdigital.fazemu.enumeration.ServicosEnum.CONSULTA_STATUS_SERVICO;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.ws.BindingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.b2wdigital.fazemu.business.client.ConsultarStatusServicoNFeClient;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.domain.CertificadoDigitalRedis;
import com.b2wdigital.fazemu.net.CustomCertSSLSocketFactory;
import com.b2wdigital.fazemu.net.NFeSOAPHeaderHandler;

import br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NFeStatusServico4;
import br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NFeStatusServico4Soap12;
import br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NfeResultMsg;
import com.b2wdigital.fazemu.utils.FazemuUtils;

@Component
public class ConsultarStatusServicoSefazSoapClient implements ConsultarStatusServicoNFeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarStatusServicoSefazSoapClient.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Override
    public NfeResultMsg nfeStatusServico(String url, NfeDadosMsg nfeDadosMsg, Integer uf) throws Exception {
        LOGGER.debug("ConsultarStatusServicoSefazSoapClient: nfeStatusServico");

        Long raizCnpjEmitente = 5886614L;	//FIXME: Verificar 
        CertificadoDigitalRedis certificadoDigitalRedis = certificadoDigitalService.getInstance(raizCnpjEmitente);

        ByteArrayInputStream truststoreInputStream = new ByteArrayInputStream(parametrosInfraRepository.getAsByteArray(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIB_TRUSTSTORE));
        char[] truststorePassword = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TRUSTSTORE_PASSWORD).toCharArray();

        SSLSocketFactory sslSocketFactory = CustomCertSSLSocketFactory.getInstance(new ByteArrayInputStream(certificadoDigitalRedis.getCertificadoDigitalByte()),
                new String(Base64.getDecoder().decode(certificadoDigitalRedis.getSenha())).toCharArray(),
                truststoreInputStream, truststorePassword);

        NFeStatusServico4 service = new NFeStatusServico4();
        NFeStatusServico4Soap12 port = service.getNFeStatusServico4Soap12();

        BindingProvider bindingProvider = (BindingProvider) port;
        Map<String, Object> requestContext = ((BindingProvider) port).getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        requestContext.put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", sslSocketFactory);
        requestContext.put("weblogic.wsee.jaxws.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        requestContext.put("com.sun.xml.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        requestContext.put("com.sun.xml.internal.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        NFeSOAPHeaderHandler.prepareHeader(bindingProvider, CONSULTA_STATUS_SERVICO.getNamespace(), uf, CONSULTA_STATUS_SERVICO.getVersao());

        return port.nfeStatusServicoNF(nfeDadosMsg);
    }

}
