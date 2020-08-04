package com.b2wdigital.nfe;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Scanner;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.ws.BindingProvider;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.b2wdigital.assinatura_digital.exception.AssinaturaDigitalException;
import com.b2wdigital.assinatura_digital.exception.LeituraCertificadoException;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.exception.InstanciacaoSocketFactoryException;
import com.b2wdigital.fazemu.net.CustomCertSSLSocketFactory;
import com.b2wdigital.fazemu.net.NFeSOAPHeaderHandler;
import com.b2wdigital.fazemu.utils.XMLUtils;

public class NFe_RECEPCAO_ASSINADO {

    private static final String SENHA_TRUSTSTORE = "SENHA_TRUSTSTORE";
    private static final String TRUSTSTORE = "TRUSTSTORE";

    private static final String SENHA_CERTIFICADO = "SENHA_CERTIFICADO";
    private static final String CERTIFICADO = "CERTIFICADO";
    private static final String XML = "XML";
    private static final String MODE = "MODE";
    private static final String RECIBO = "RECIBO";
    
    static String truststoreSenha = null;
    static String truststoreCaminho = null;
    static String certificadoSenha = null;
    static String certificadoCaminho = null;
    static String xmlNFeCaminho = null;
    static String mode = null;
    static String recibo = null;
    
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, TransformerConfigurationException, TransformerException, LeituraCertificadoException, AssinaturaDigitalException, InstanciacaoSocketFactoryException, JAXBException, ParseException {
    	
    	Scanner scanner = new Scanner(System.in);
        for (int i = 0; i < args.length; i += 2) {
            String key = args[i];
            String value = args[i + 1];
            if (value.equalsIgnoreCase("?")) {
                System.out.println("Digite um valor para " + key + ":");
                value = scanner.nextLine();
            }
            if (SENHA_CERTIFICADO.equalsIgnoreCase(key)) {
                certificadoSenha = value;
            } else if (CERTIFICADO.equalsIgnoreCase(key)) {
                certificadoCaminho = value;
            } else if (SENHA_TRUSTSTORE.equalsIgnoreCase(key)) {
                truststoreSenha = value;
            } else if (TRUSTSTORE.equalsIgnoreCase(key)) {
                truststoreCaminho = value;
            } else if (XML.equalsIgnoreCase(key)) {
                xmlNFeCaminho = value;
            } else if (MODE.equalsIgnoreCase(key)) {
                mode = value;
            } else if (RECIBO.equalsIgnoreCase(key)) {
                recibo = value;
            }
        }
        scanner.close();

		autorizarLoteNFe();
    }
    
	/**
	 * Autorizar Lote NFe
	 * 
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 * @throws LeituraCertificadoException
	 * @throws AssinaturaDigitalException
	 * @throws InstanciacaoSocketFactoryException
	 * @throws JAXBException
	 */
	private static void autorizarLoteNFe() throws ParserConfigurationException, SAXException, IOException,
			TransformerConfigurationException, TransformerException, LeituraCertificadoException,
			AssinaturaDigitalException, InstanciacaoSocketFactoryException, JAXBException {
		
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        
        InputStream isFromFile1 = XMLUtils.getStreamFromFile("/opt/workspace/fazemu/doc/nf-e-emitidas-umbrella/epec/nf_hml_118_epec.xml");
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document documentLote = builder.parse(isFromFile1);

        FileInputStream certInputStream = new FileInputStream(certificadoCaminho);
        FileInputStream truststoreInputStream = new FileInputStream(truststoreCaminho);
        SSLSocketFactory sslSocketFactory = CustomCertSSLSocketFactory.getInstance(certInputStream, certificadoSenha.toCharArray(), truststoreInputStream, truststoreSenha.toCharArray());

        String url = "https://homologacao.nfe.fazenda.sp.gov.br/ws/nfeautorizacao4.asmx";
        String namespace = ServicosEnum.AUTORIZACAO_NFE.getNamespace();
        int codigoUFOrigemEmissor = 35;
        String versaoDados = ServicosEnum.AUTORIZACAO_NFE.getVersao();

        br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NFeAutorizacao4 service = new br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NFeAutorizacao4();
        br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NFeAutorizacao4Soap12 port = service.getNFeAutorizacao4Soap12();

        BindingProvider bindingProvider = (BindingProvider) port;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", sslSocketFactory);
        bindingProvider.getRequestContext().put("weblogic.wsee.jaxws.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        bindingProvider.getRequestContext().put("com.sun.xml.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        NFeSOAPHeaderHandler.prepareHeader(bindingProvider, namespace, codigoUFOrigemEmissor, versaoDados);

        br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeDadosMsg nfeDadosMsg = new br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeDadosMsg();

		//remove namespaces
        documentLote = XMLUtils.cleanNameSpace(documentLote);
		XMLUtils.showXML(documentLote);
		
        nfeDadosMsg.getContent().add(documentLote.getDocumentElement());
        br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeResultMsg result = port.nfeAutorizacaoLote(nfeDadosMsg);

        JAXBContext context = JAXBContext.newInstance(br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeResultMsg.class);
        Marshaller marshaller = context.createMarshaller();
        System.out.println("\n\nResultado envio ==================================================");
        marshaller.marshal(result, System.out);
    }

}
