package com.b2wdigital.nfe;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Scanner;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.BindingProvider;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.b2wdigital.assinatura_digital.CertificadoDigital;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.net.CustomCertSSLSocketFactory;
import com.b2wdigital.fazemu.net.NFeSOAPHeaderHandler;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;

/**
 * EPEC de NFe
 * 
 * @author Rodolpho Picolo <rodolpho.picolo@b2wdigital.com>
 */
public class NFe_EPEC {

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

    public static void main(String[] args) throws Exception {

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

		enviarEPEC();
    }
    
	/**
	 * EPEC NFe
	 * @throws Exception 
	 */
	public static void enviarEPEC() throws Exception {
		
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        InputStream isFromFile = XMLUtils.getStreamFromFile(xmlNFeCaminho);

        Document docEPEC = builder.parse(isFromFile);

        Element nfeElementRascunho = docEPEC.getDocumentElement();

        DOMSource source = new DOMSource(nfeElementRascunho);

        
        System.out.println("\n\nDocumento não assinado: ===========================================");
        javax.xml.transform.Result output = new StreamResult(System.out);

        TransformerFactory transformFactory = TransformerFactory.newInstance();
        Transformer transformer = transformFactory.newTransformer();
        transformer.transform(source, output);

        FileInputStream certInputStream = new FileInputStream(certificadoCaminho);
        CertificadoDigital certificado = CertificadoDigital.getInstance(certInputStream, certificadoSenha.toCharArray());

        //Assina o EPEC
        XMLUtils.cleanNameSpace(docEPEC);
        FazemuUtils.signXml(docEPEC, certificado, ServicosEnum.RECEPCAO_EVENTO_EPEC);
        
        System.out.println("\n\n\n\nDocumento assinado: ===========================================\n");
        source = new DOMSource(docEPEC);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        output = new StreamResult(baos);

        transformer.transform(source, output);
        
        byte[] bytesXML = baos.toByteArray();
        System.out.write(bytesXML);

        certInputStream = new FileInputStream(certificadoCaminho);
        FileInputStream truststoreInputStream = new FileInputStream(truststoreCaminho);
        SSLSocketFactory sslSocketFactory = CustomCertSSLSocketFactory.getInstance(certInputStream, certificadoSenha.toCharArray(), truststoreInputStream, truststoreSenha.toCharArray());

        nfeRecepcaoEvento(sslSocketFactory, docEPEC);
    }
    
    public static void nfeRecepcaoEvento(SSLSocketFactory sslSocketFactory, Document docEPEC) throws Exception {
    	
        //Cabecalho
        String url = "https://hom.nfe.fazenda.gov.br/NFeRecepcaoEvento4/NFeRecepcaoEvento4.asmx";

        String namespace = "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4";
        int codigoUFOrigemEmissor = 35;
        String versaoDados = ServicosEnum.RECEPCAO_EVENTO_EPEC.getVersao();

        br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NFeRecepcaoEvento4 service = new br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NFeRecepcaoEvento4();
        br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NFeRecepcaoEvento4Soap12 port = service.getNFeRecepcaoEvento4Soap12();

        BindingProvider bindingProvider = (BindingProvider) port;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", sslSocketFactory);
        bindingProvider.getRequestContext().put("weblogic.wsee.jaxws.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        bindingProvider.getRequestContext().put("com.sun.xml.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        bindingProvider.getRequestContext().put("com.sun.xml.internal.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sslSocketFactory);
        NFeSOAPHeaderHandler.prepareHeader(bindingProvider, namespace, codigoUFOrigemEmissor, versaoDados);
        
		br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg nfeDadosMsg = new br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg();
        nfeDadosMsg.getContent().add(docEPEC.getDocumentElement());

        // Documento com cabecalho soap
		Document docPrint = XMLUtils.createNewDocument();
		nfeRecepcaoEventoContext().createMarshaller().marshal(nfeDadosMsg, docPrint);
		System.out.println("\n\ndocPrint ==================================================");
		XMLUtils.showXML(docPrint);
		
		//Integração SEFAZ        
        br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg result = port.nfeRecepcaoEvento(nfeDadosMsg);

        JAXBContext context = JAXBContext.newInstance(br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg.class);
        Marshaller marshaller = context.createMarshaller();
        System.out.println("\n\nResultado envio ==================================================");
        marshaller.marshal(result, System.out);
    }
    
    public static JAXBContext nfeRecepcaoEventoContext() throws JAXBException {
        return JAXBContext.newInstance(
        		br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg.class
                , br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg.class
        );
    }
    
}