package com.b2wdigital.fazemu.integration.client.soap.sefaz;

import static com.b2wdigital.fazemu.enumeration.ServicosEnum.CONSULTA_PROTOCOLO;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.ws.BindingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.b2wdigital.fazemu.business.client.ConsultarProtocoloClient;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.domain.CertificadoDigitalRedis;
import com.b2wdigital.fazemu.exception.FazemuClientException;
import com.b2wdigital.fazemu.net.CustomCertSSLSocketFactory;
import com.b2wdigital.fazemu.net.NFeSOAPHeaderHandler;
import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;
import com.b2wdigital.fazemu.utils.FazemuUtils;

import br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NFeConsultaProtocolo4;
import br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NFeConsultaProtocolo4Soap12;
import br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeResultMsg;

@Component
public class ConsultarProtocoloSefazSoapClient implements ConsultarProtocoloClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarProtocoloSefazSoapClient.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Override
    public NfeResultMsg nfeConsultarProtocolo(String url, ChaveAcessoNFe chaveAcesso, String versao, NfeDadosMsg nfeDadosMsg) throws Exception {
        try {
            LOGGER.debug("ConsultarProtocoloSefazSoapClient: nfeConsultarProtocolo");

            long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(Long.valueOf(chaveAcesso.cnpjCpf));
            if (raizCnpjEmitente != 776574
                    && raizCnpjEmitente != 5886614) {
                raizCnpjEmitente = 776574; //TODO Validar melhor maneira de utilizar um certificado "generico" para consulta de status de NFe de terceiros
            }

            CertificadoDigitalRedis certificadoDigitalRedis = certificadoDigitalService.getInstance(raizCnpjEmitente);

            ByteArrayInputStream truststoreInputStream = new ByteArrayInputStream(parametrosInfraRepository.getAsByteArray(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIB_TRUSTSTORE));
            char[] truststorePassword = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TRUSTSTORE_PASSWORD).toCharArray();

            SSLSocketFactory sslSocketFactory = CustomCertSSLSocketFactory.getInstance(new ByteArrayInputStream(certificadoDigitalRedis.getCertificadoDigitalByte()),
                    new String(Base64.getDecoder().decode(certificadoDigitalRedis.getSenha())).toCharArray(),
                    truststoreInputStream, truststorePassword);

            NFeConsultaProtocolo4 service = new NFeConsultaProtocolo4();
            NFeConsultaProtocolo4Soap12 port = service.getNFeConsultaProtocolo4Soap12();

            BindingProvider bindingProvider = (BindingProvider) port;
            bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
            bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", sslSocketFactory);
            bindingProvider.getRequestContext().put("weblogic.wsee.jaxws.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
            bindingProvider.getRequestContext().put("com.sun.xml.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
            bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
            NFeSOAPHeaderHandler.prepareHeader(bindingProvider, CONSULTA_PROTOCOLO.getNamespace(), Integer.parseInt(chaveAcesso.cUF), versao);

            return port.nfeConsultaNF(nfeDadosMsg);
        } catch (Exception e) {
            throw new FazemuClientException(e);
        }
    }

}
