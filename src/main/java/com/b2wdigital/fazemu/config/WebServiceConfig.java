package com.b2wdigital.fazemu.config;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2winc.corpserv.message.filter.ExceptionHandlerFilter;
import com.b2wdigital.fazemu.filter.AuthorizationFilter;

/**
 * WebService Config.
 * 
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {
	
	@Autowired 
	private AuthorizationFilter authorizationFilter;
	
	@Bean
	public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
		MessageDispatcherServlet servlet = new MessageDispatcherServlet();
		servlet.setApplicationContext(applicationContext);
		servlet.setTransformWsdlLocations(true);
		return new ServletRegistrationBean<MessageDispatcherServlet>(servlet, "/ws/*");
	}
	
	
	
	/**
	 * Consulta NFe
	 */
	@Bean(name = "consSitNFe")
	public DefaultWsdl11Definition defaultWsdl11DefinitionConsultaNFe(@Qualifier("consSitNFeSchema") XsdSchema schema) {
	    DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
	    wsdl11Definition.setPortTypeName("ConsSitNFeSchemaPort");
	    wsdl11Definition.setLocationUri("/ws/consSitNFe");
	    wsdl11Definition.setTargetNamespace(FazemuUtils.NAMESPACE_URI);
	    wsdl11Definition.setSchema(schema);
	    return wsdl11Definition;
	}
	
	@Bean(name = "consSitNFeSchema")
	public XsdSchema consSitNFeSchema() {
	    return new SimpleXsdSchema(new ClassPathResource("xsd/leiauteConsSitNFe_v4.00.xsd"));
	}
	
	/**
	 * Consulta Status Servico
	 */
	@Bean(name = "consStatServ")
	public DefaultWsdl11Definition defaultWsdl11DefinitionConsultaStatusServico(@Qualifier("consStatServSchema") XsdSchema schema) {
	    DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
	    wsdl11Definition.setPortTypeName("consStatServSchemaPort");
	    wsdl11Definition.setLocationUri("/ws/consStatServ");
	    wsdl11Definition.setTargetNamespace(FazemuUtils.NAMESPACE_URI);
	    wsdl11Definition.setSchema(schema);
	    return wsdl11Definition;
	}
	
	@Bean(name = "consStatServSchema")
	public XsdSchema consStatServSchema() {
	    return new SimpleXsdSchema(new ClassPathResource("xsd/leiauteConsStatServ_v4.00.xsd"));
	}
	
	/**
	 * Recepcao de Evento - Cancelamento
	 */
	@Bean(name = "envEventoCancelamento")
	public DefaultWsdl11Definition defaultWsdl11DefinitionRecepcaoEventoCancelamento(@Qualifier("envEventoCancelamentoSchema") XsdSchema schema) {
	    DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
	    wsdl11Definition.setPortTypeName("envEventoCancelamentoSchemaPort");
	    wsdl11Definition.setLocationUri("/ws/envEventoCancelamento");
	    wsdl11Definition.setTargetNamespace(FazemuUtils.NAMESPACE_URI);
	    wsdl11Definition.setSchema(schema);
	    return wsdl11Definition;
	}
	
	@Bean(name = "envEventoCancelamentoSchema")
	public XsdSchema envEventoCancelamentoSchema() {
	    return new SimpleXsdSchema(new ClassPathResource("xsd/leiauteEventoCancelamentoNFe_v1.00.xsd"));
	}
	
	/**
	 * Recepcao de Evento - Carta de Correcao
	 */
	@Bean(name = "envEventoCartaCorrecao")
	public DefaultWsdl11Definition defaultWsdl11DefinitionRecepcaoEventoCartaCorrecao(@Qualifier("envEventoCartaCorrecaoSchema") XsdSchema schema) {
	    DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
	    wsdl11Definition.setPortTypeName("envEventoCartaCorrecaoSchemaPort");
	    wsdl11Definition.setLocationUri("/ws/envEventoCartaCorrecao");
	    wsdl11Definition.setTargetNamespace(FazemuUtils.NAMESPACE_URI);
	    wsdl11Definition.setSchema(schema);
	    return wsdl11Definition;
	}
	
	@Bean(name = "envEventoCartaCorrecaoSchema")
	public XsdSchema envEventoCartaCorrecaoSchema() {
	    return new SimpleXsdSchema(new ClassPathResource("xsd/leiauteConfRecebto_v1.00.xsd"));
	}
	
	/**
	 * Recepcao de Evento - Manifestacao
	 */
	@Bean(name = "envEventoManifestacao")
	public DefaultWsdl11Definition defaultWsdl11DefinitionRecepcaoEventoManifestacao(@Qualifier("envEventoManifestacaoSchema") XsdSchema schema) {
	    DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
	    wsdl11Definition.setPortTypeName("envEventoManifestacaoSchemaPort");
	    wsdl11Definition.setLocationUri("/ws/envEventoManifestacao");
	    wsdl11Definition.setTargetNamespace(FazemuUtils.NAMESPACE_URI);
	    wsdl11Definition.setSchema(schema);
	    return wsdl11Definition;
	}
	
	@Bean(name = "envEventoManifestacaoSchema")
	public XsdSchema envEventoManifestacaoSchema() {
	    return new SimpleXsdSchema(new ClassPathResource("xsd/leiauteCCe_v1.00.xsd"));
	}
	
	/**
	 * Inutilizacao de NFe
	 */
	@Bean(name = "envInutNFe")
	public DefaultWsdl11Definition defaultWsdl11DefinitionInutilizacao(@Qualifier("inutNFeSchema") XsdSchema schema) {
	    DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
	    wsdl11Definition.setPortTypeName("inutNFeSchemaPort");
	    wsdl11Definition.setLocationUri("/ws/inutNFe");
	    wsdl11Definition.setTargetNamespace(FazemuUtils.NAMESPACE_URI);
	    wsdl11Definition.setSchema(schema);
	    return wsdl11Definition;
	}
	
	@Bean(name = "inutNFeSchema")
	public XsdSchema inutNFeSchema() {
	    return new SimpleXsdSchema(new ClassPathResource("xsd/leiauteInutNFe_v4.00.xsd"));
	}
	
	/**
	 * Autorizacao de NFe
	 */
	@Bean(name = "nfe")
	public DefaultWsdl11Definition defaultWsdl11DefinitionAutorizacao(@Qualifier("nfeSchema") XsdSchema schema) {
	    DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
	    wsdl11Definition.setPortTypeName("nfeSchemaPort");
	    wsdl11Definition.setLocationUri("/ws/nfe");
	    wsdl11Definition.setTargetNamespace(FazemuUtils.NAMESPACE_URI);
	    wsdl11Definition.setSchema(schema);
	    return wsdl11Definition;
	}
	
	@Bean(name = "nfeSchema")
	public XsdSchema nfeSchema() {
	    return new SimpleXsdSchema(new ClassPathResource("xsd/leiauteNFe_v4.00.xsd"));
	}
	
	/**
	 * Distribuicao Documentos Fiscais Eletronicos
	 */
	@Bean(name = "distribuicaoDFe")
	public DefaultWsdl11Definition defaultWsdl11DefinitionDistribuicaoDFeSchema(@Qualifier("distribuicaoDFeSchema") XsdSchema schema) {
	    DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
	    wsdl11Definition.setPortTypeName("distribuicaoDFeSchemaPort");
	    wsdl11Definition.setLocationUri("/ws/distribuicaoDFe");
	    wsdl11Definition.setTargetNamespace(FazemuUtils.NAMESPACE_URI);
	    wsdl11Definition.setSchema(schema);
	    return wsdl11Definition;
	}
	
	@Bean(name = "distribuicaoDFeSchema")
	public XsdSchema distribuicaoDFeSchema() {
	    return new SimpleXsdSchema(new ClassPathResource("xsd/distDFeInt_v1.01.xsd"));
	}
	
	/**
	 * Recepcao de Evento - EPEC
	 */
	@Bean(name = "envEventoEpec")
	public DefaultWsdl11Definition defaultWsdl11DefinitionRecepcaoEventoEpec(@Qualifier("envEventoEpecSchema") XsdSchema schema) {
	    DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
	    wsdl11Definition.setPortTypeName("envEventoEpecSchemaPort");
	    wsdl11Definition.setLocationUri("/ws/envEventoEpec");
	    wsdl11Definition.setTargetNamespace(FazemuUtils.NAMESPACE_URI);
	    wsdl11Definition.setSchema(schema);
	    return wsdl11Definition;
	}
	
	@Bean(name = "envEventoEpecSchema")
	public XsdSchema envEventoEpecSchema() {
	    return new SimpleXsdSchema(new ClassPathResource("xsd/leiauteEPEC_v1.00.xsd"));
	}

	
    @Bean(name = "registerExceptionHandlerFilter")
    public FilterRegistrationBean registerExceptionHandlerFilter(){
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new ExceptionHandlerFilter());
        filterRegistrationBean.setInitParameters(Collections.singletonMap("jsonLogFormat", "true"));
        filterRegistrationBean.setOrder(1);
        return filterRegistrationBean;
    }
    
    @Bean(name="registerAuthorizationFilter")
    public FilterRegistrationBean registerAuthorizationFilter(){
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(authorizationFilter);
        
        filterRegistrationBean.setOrder(2);
        return filterRegistrationBean;
    }
	   
    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {

        // register global interceptor
        interceptors.add(new GlobalEndpointInterceptor());

//        // register endpoint specific interceptor
//        interceptors.add(new PayloadRootSmartSoapEndpointInterceptor(
//                new GlobalEndpointInterceptor(),
//                "http://www.portalfiscal.inf.br/nfe/wsdl/NFeAutorizacao4",
//                "nfeDadosMsg"));
    }
	
}
