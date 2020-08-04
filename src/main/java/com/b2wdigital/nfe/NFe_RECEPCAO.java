package com.b2wdigital.nfe;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Scanner;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.BindingProvider;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.b2wdigital.assinatura_digital.AssinaturaDigital;
import com.b2wdigital.assinatura_digital.CertificadoDigital;
import com.b2wdigital.assinatura_digital.exception.AssinaturaDigitalException;
import com.b2wdigital.assinatura_digital.exception.LeituraCertificadoException;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.exception.InstanciacaoSocketFactoryException;
import com.b2wdigital.fazemu.net.CustomCertSSLSocketFactory;
import com.b2wdigital.fazemu.net.NFeSOAPHeaderHandler;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TConsReciNFe;
import com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TEnviNFe;

/**
 * Recepção de Lote / Consulta Processamento de Lote
 * 
 * @author Rodolpho Picolo <rodolpho.picolo@b2wdigital.com>
 */
public class NFe_RECEPCAO {

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

        if (mode.equalsIgnoreCase("AUTORIZAR")) {
    		autorizarNFe();
        }
        
        else if (mode.equalsIgnoreCase("CONSULTAR")) {
			consultarRetornoNFe();
        }

    }
    
	/**
	 * Autorizar NFe
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
	private static void autorizarNFe() throws ParserConfigurationException, SAXException, IOException,
			TransformerConfigurationException, TransformerException, LeituraCertificadoException,
			AssinaturaDigitalException, InstanciacaoSocketFactoryException, JAXBException {
		
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        InputStream isFromFile = XMLUtils.getStreamFromFile(xmlNFeCaminho);

        Document xmlDocRascunho = builder.parse(isFromFile);

        Element nfeElementRascunho = xmlDocRascunho.getDocumentElement();

        DOMSource source = new DOMSource(nfeElementRascunho);

        
        System.out.println("\n\nDocumento não assinado: ===========================================");
        javax.xml.transform.Result output = new StreamResult(System.out);

        TransformerFactory transformFactory = TransformerFactory.newInstance();
        Transformer transformer = transformFactory.newTransformer();
        transformer.transform(source, output);

        FileInputStream certInputStream = new FileInputStream(certificadoCaminho);
        CertificadoDigital certificado = CertificadoDigital.getInstance(certInputStream, certificadoSenha.toCharArray());

        AssinaturaDigital.assinarNFe(xmlDocRascunho, certificado);

        
        System.out.println("\n\n\n\nDocumento assinado: ===========================================\n");
        source = new DOMSource(xmlDocRascunho);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        output = new StreamResult(baos);

        transformer.transform(source, output);
        
        byte[] bytesXML = baos.toByteArray();
        System.out.write(bytesXML);

        certInputStream = new FileInputStream(certificadoCaminho);
        FileInputStream truststoreInputStream = new FileInputStream(truststoreCaminho);
        SSLSocketFactory sslSocketFactory = CustomCertSSLSocketFactory.getInstance(certInputStream, certificadoSenha.toCharArray(), truststoreInputStream, truststoreSenha.toCharArray());

        
        System.out.println("\n\n\n\n=====================================================================\n\n\n\n\n");
        nfeAutorizacaoLote(sslSocketFactory, xmlDocRascunho);
    }
    
    private static void nfeAutorizacaoLote(SSLSocketFactory sslSocketFactory, Document document) throws JAXBException, ParserConfigurationException {
        com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TNFe tNFe;
        
        {
            JAXBContext context = JAXBContext.newInstance(com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TNFe.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            tNFe = (com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TNFe) unmarshaller.unmarshal(document);
        }

        TEnviNFe tEnviNFe = new TEnviNFe();
        tEnviNFe.setIdLote("3");
        tEnviNFe.setIndSinc("0");
        tEnviNFe.setVersao(ServicosEnum.AUTORIZACAO_NFE.getVersao());
        tEnviNFe.getNFe().add(tNFe);

        String url = "https://homologacao.nfe.fazenda.sp.gov.br/ws/nfeautorizacao4.asmx";
        String namespace = "http://www.portalfiscal.inf.br/nfe/wsdl/NFeAutorizacao4";
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

		Document xmlTEnviNFe;
		
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			xmlTEnviNFe = builder.newDocument();

			JAXBContext context = JAXBContext.newInstance(TEnviNFe.class);
			Marshaller marshaller = context.createMarshaller();
			marshaller.marshal(tEnviNFe, xmlTEnviNFe);
		}
		
		//remove namespaces
		xmlTEnviNFe = XMLUtils.cleanNameSpace(xmlTEnviNFe);
		
        nfeDadosMsg.getContent().add(xmlTEnviNFe.getDocumentElement());
        {
            JAXBContext context = JAXBContext.newInstance(br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeDadosMsg.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(nfeDadosMsg, System.out);
        }

        br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeResultMsg result = port.nfeAutorizacaoLote(nfeDadosMsg);

        JAXBContext context = JAXBContext.newInstance(br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeResultMsg.class);
        Marshaller marshaller = context.createMarshaller();
        System.out.println("\n\nResultado envio ==================================================");
        marshaller.marshal(result, System.out);
    }

	/**
	 * Consultar Retorno NFe
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
	private static void consultarRetornoNFe() throws ParserConfigurationException, SAXException, IOException,
			TransformerConfigurationException, TransformerException, LeituraCertificadoException,
			AssinaturaDigitalException, InstanciacaoSocketFactoryException, JAXBException {

		FileInputStream certInputStream = new FileInputStream(certificadoCaminho);
		FileInputStream truststoreInputStream = new FileInputStream(truststoreCaminho);
		SSLSocketFactory socketFactory = CustomCertSSLSocketFactory.getInstance(certInputStream, certificadoSenha.toCharArray(), truststoreInputStream, truststoreSenha.toCharArray());

		nfeRetornoAutorizacaoLote(socketFactory, recibo);
	}
    
	private static void nfeRetornoAutorizacaoLote(SSLSocketFactory sslSocketFactory, String numeroRecibo) throws JAXBException, ParserConfigurationException, TransformerException {
        if (numeroRecibo == null || numeroRecibo.trim().isEmpty()) {
            numeroRecibo = "351000126075040";
        }

        TConsReciNFe tConsReciNFe = new TConsReciNFe();
        tConsReciNFe.setNRec(numeroRecibo);
        tConsReciNFe.setTpAmb("2");
        tConsReciNFe.setVersao(ServicosEnum.AUTORIZACAO_NFE.getVersao());

        Document xmlTConsReciNFe;
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            xmlTConsReciNFe = builder.newDocument();

            JAXBContext context = JAXBContext.newInstance(TConsReciNFe.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(tConsReciNFe, xmlTConsReciNFe);
        }

        String url = "https://homologacao.nfe.fazenda.sp.gov.br/ws/nferetautorizacao4.asmx";
        String namespace = "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRetAutorizacao4";
        int codigoUFOrigemEmissor = 33;
        String versaoDados = ServicosEnum.AUTORIZACAO_NFE.getVersao();

        br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NFeRetAutorizacao4 service = new br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NFeRetAutorizacao4();
        br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NFeRetAutorizacao4Soap12 port = service.getNFeRetAutorizacao4Soap12();
        br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NfeDadosMsg nfeDadosMsg = new br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NfeDadosMsg();
        
        nfeDadosMsg.getContent().add(xmlTConsReciNFe.getDocumentElement());
        
       	XMLUtils.showXML(xmlTConsReciNFe);

        BindingProvider bindingProvider = (BindingProvider) port;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", sslSocketFactory);
        bindingProvider.getRequestContext().put("weblogic.wsee.jaxws.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        bindingProvider.getRequestContext().put("com.sun.xml.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        NFeSOAPHeaderHandler.prepareHeader(bindingProvider, namespace, codigoUFOrigemEmissor, versaoDados);

        br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NfeResultMsg result = port.nfeRetAutorizacaoLote(nfeDadosMsg);

        JAXBContext context = JAXBContext.newInstance(br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NfeResultMsg.class);
        Marshaller marshaller = context.createMarshaller();
        System.out.println("\n\nResultado consulta recibo ==================================================");
        marshaller.marshal(result, System.out);
    }
	
}
