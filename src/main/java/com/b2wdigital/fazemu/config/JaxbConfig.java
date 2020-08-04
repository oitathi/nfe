package com.b2wdigital.fazemu.config;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author dailton.almeida
 */
@Configuration
public class JaxbConfig {

    private static final Log LOGGER = LogFactory.getLog(JaxbConfig.class);

    //contexts
    @Bean("nfeAutorizacaoContext")
    public JAXBContext nfeAutorizacaoContext() throws JAXBException {
        LOGGER.debug("Injetando nfeAutorizacaoContext");
        return JAXBContext.newInstance(
                br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeDadosMsg.class,
                br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeResultMsg.class,
                com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TEnviNFe.class,
                com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TNFe.class,
                com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TRetEnviNFe.class
        );
    }

    @Bean("nfeRetAutorizacaoContext")
    public JAXBContext nfeRetAutorizacaoContext() throws JAXBException {
        LOGGER.debug("Injetando nfeRetAutorizacaoContext");
        return JAXBContext.newInstance(
                br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NfeDadosMsg.class,
                br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NfeResultMsg.class,
                com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TConsReciNFe.class,
                com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.class,
                com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc.class,
                com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TRetConsReciNFe.class
        );
    }

    @Bean("nfeStatusServicoContext")
    public JAXBContext nfeStatusServicoContext() throws JAXBException {
        LOGGER.debug("Injetando nfeStatusServicoContext");
        return JAXBContext.newInstance(
                br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NfeDadosMsg.class,
                br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NfeResultMsg.class,
                com.b2wdigital.nfe.schema.v4v160b.consStatServ_v4.TConsStatServ.class,
                com.b2wdigital.nfe.schema.v4v160b.consStatServ_v4.TRetConsStatServ.class
        );
    }

    @Bean("nfeConsultarProtocoloContext")
    public JAXBContext nfeConsultarProtocoloContext() throws JAXBException {
        LOGGER.debug("Injetando nfeConsultarProtocoloContext");
        return JAXBContext.newInstance(
                br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeDadosMsg.class,
                br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeResultMsg.class,
                com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TConsSitNFe.class,
                com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TRetConsSitNFe.class,
                com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TRetEvento.class
        );
    }

    @Bean("nfeInutilizacaoContext")
    public JAXBContext nfeInutilizacaoContext() throws JAXBException {
        LOGGER.debug("Injetando nfeInutilizacaoContext");
        return JAXBContext.newInstance(
                br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeDadosMsg.class,
                br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeResultMsg.class,
                com.b2wdigital.nfe.schema.v4v160b.inutNFe_v4.TInutNFe.class,
                com.b2wdigital.nfe.schema.v4v160b.inutNFe_v4.TRetInutNFe.class
        );
    }

    @Bean("nfeRecepcaoEventoContext")
    public JAXBContext nfeRecepcaoEventoContext() throws JAXBException {
        LOGGER.debug("Injetando nfeRecepcaoEventoContext");
        return JAXBContext.newInstance(
                br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg.class,
                br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg.class
        );
    }

    @Bean("nfeRecepcaoEventoEpecContext")
    public JAXBContext nfeRecepcaoEventoEpecContext() throws JAXBException {
        LOGGER.debug("Injetando nfeRecepcaoEventoEpecContext");
        return JAXBContext.newInstance(
                com.b2wdigital.nfe.schema.v4v160b.epec.EPEC_v1.TEvento.class,
                com.b2wdigital.nfe.schema.v4v160b.epec.EPEC_v1.TRetEnvEvento.class,
                com.b2wdigital.nfe.schema.v4v160b.epec.EPEC_v1.TRetEvento.class
        );
    }

    @Bean("nfeRecepcaoEventoCancelamentoContext")
    public JAXBContext nfeRecepcaoEventoCancelamentoContext() throws JAXBException {
        LOGGER.debug("Injetando nfeRecepcaoEventoCancelamentoContext");
        return JAXBContext.newInstance(
                com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TEvento.class,
                com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TRetEnvEvento.class,
                com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TRetEvento.class
        );
    }

    @Bean("nfeRecepcaoEventoCartaCorrecaoContext")
    public JAXBContext nfeRecepcaoEventoCartaCorrecaoContext() throws JAXBException {
        LOGGER.debug("Injetando nfeRecepcaoEventoCartaCorrecaoContext");
        return JAXBContext.newInstance(
                com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TEvento.class,
                com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TRetEnvEvento.class,
                com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TretEvento.class,
                com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TProcEvento.class
        );
    }

    @Bean("nfeRecepcaoEventoManifestacaoContext")
    public JAXBContext nfeRecepcaoEventoManifestacaoContext() throws JAXBException {
        LOGGER.debug("Injetando nfeRecepcaoEventoManifestacaoContext");
        return JAXBContext.newInstance(
                com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TEvento.class,
                com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TRetEnvEvento.class,
                com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TretEvento.class
        );
    }

    @Bean("nfeDistribuicaoDocumentosContext")
    public JAXBContext nfeDistribuicaoDocumentosContext() throws JAXBException {
        LOGGER.debug("Injetando nfeDistribuicaoDocumentosContext");
        return JAXBContext.newInstance(
                br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresse.class,
                com.b2wdigital.nfe.schema.v4v160b.distdfe.distDFeInt_v1.DistDFeInt.class,
                com.b2wdigital.nfe.schema.v4v160b.distdfe.retDistDFeInt_v1.RetDistDFeInt.class,
                com.b2wdigital.nfe.schema.v4v160b.distdfe.resNFe_v1.ResNFe.class
        );
    }

}
