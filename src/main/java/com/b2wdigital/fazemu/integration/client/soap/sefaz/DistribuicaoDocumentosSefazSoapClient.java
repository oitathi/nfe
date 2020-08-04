package com.b2wdigital.fazemu.integration.client.soap.sefaz;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.ws.BindingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.b2wdigital.fazemu.business.client.DistribuicaoDocumentosClient;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.domain.CertificadoDigitalRedis;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.net.CustomCertSSLSocketFactory;
import com.b2wdigital.fazemu.net.NFeSOAPHeaderHandler;
import com.b2wdigital.fazemu.utils.FazemuUtils;

import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NFeDistribuicaoDFe;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NFeDistribuicaoDFeSoap;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresse.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresseResponse;

@Component
public class DistribuicaoDocumentosSefazSoapClient implements DistribuicaoDocumentosClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistribuicaoDocumentosSefazSoapClient.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    /**
     * @param url
     * @param nfeDadosMsg
     * @param uf
     * @param cnpj
     * @return
     * @throws Exception
     */
    @Override
    public NfeDistDFeInteresseResponse nfeDistribuicaoDocumentos(String url, NfeDadosMsg nfeDadosMsg, Integer uf, Long cnpj) throws Exception {
        LOGGER.debug("DistribuicaoDocumentosSefazSoapClient: nfeDistribuicaoDocumentos");

        long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(cnpj);
        CertificadoDigitalRedis certificadoDigitalRedis = certificadoDigitalService.getInstance(raizCnpjEmitente);

        ByteArrayInputStream truststoreInputStream = new ByteArrayInputStream(parametrosInfraRepository.getAsByteArray(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIB_TRUSTSTORE));
        char[] truststorePassword = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TRUSTSTORE_PASSWORD).toCharArray();

        SSLSocketFactory sslSocketFactory = CustomCertSSLSocketFactory.getInstance(new ByteArrayInputStream(certificadoDigitalRedis.getCertificadoDigitalByte()),
                new String(Base64.getDecoder().decode(certificadoDigitalRedis.getSenha())).toCharArray(),
                truststoreInputStream, truststorePassword);

        NFeDistribuicaoDFe service = new NFeDistribuicaoDFe();
        NFeDistribuicaoDFeSoap port = service.getNFeDistribuicaoDFeSoap12();

        BindingProvider bindingProvider = (BindingProvider) port;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", sslSocketFactory);
        bindingProvider.getRequestContext().put("weblogic.wsee.jaxws.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        bindingProvider.getRequestContext().put("com.sun.xml.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        NFeSOAPHeaderHandler.prepareHeader(bindingProvider, ServicosEnum.DISTRIBUICAO_DFE.getNamespace(), uf, ServicosEnum.DISTRIBUICAO_DFE.getVersao());

        NfeDistDFeInteresseResponse response = new NfeDistDFeInteresseResponse();
        response.setNfeDistDFeInteresseResult(port.nfeDistDFeInteresse(nfeDadosMsg));

        return response;
    }

}
