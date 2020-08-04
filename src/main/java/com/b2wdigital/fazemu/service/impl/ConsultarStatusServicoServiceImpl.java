package com.b2wdigital.fazemu.service.impl;

import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.b2wdigital.fazemu.business.repository.CodigoRetornoAutorizadorRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.ConsultarStatusServicoService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.CodigoRetornoAutorizador;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.exception.FazemuServiceUrlNotFoundException;
import com.b2wdigital.fazemu.integration.client.soap.sefaz.ConsultarStatusServicoSefazSoapClient;
import com.b2wdigital.fazemu.utils.DateUtils;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consStatServ_v4.TConsStatServ;
import com.b2wdigital.nfe.schema.v4v160b.consStatServ_v4.TRetConsStatServ;

import br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NfeResultMsg;

@Service
public class ConsultarStatusServicoServiceImpl extends AbstractNFeServiceImpl implements ConsultarStatusServicoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarStatusServicoServiceImpl.class);

    @Autowired
    private CodigoRetornoAutorizadorRepository codigoRetornoAutorizadorRepository;
    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;
    @Autowired
    private ConsultarStatusServicoSefazSoapClient consultarStatusServicoNFeClient;
    @Autowired
    private RedisOperationsService redisOperationsService;
    @Autowired
    @Qualifier("nfeStatusServicoContext")
    private JAXBContext context;

    @Override
    public NfeResultMsg process(NfeDadosMsg request, Integer tipoEmissao) throws Exception {
        Integer ufIBGE = NumberUtils.INTEGER_ZERO;
        String versao = StringUtils.EMPTY;

        try {
            LOGGER.debug("ConsultarStatusServicoServiceImpl: process");

            String statusFazemu = (String) redisOperationsService.getKeyValue(FazemuUtils.FAZEMU_NFE_STATUS);
            if (!"ON".equals(statusFazemu)) {
                throw new FazemuServiceException("Sistema Indisponível no momento.");
            }

            //Pega documento pela raiz
            Document docConsStatServ = getDocument(request);
            TConsStatServ tConsStatServNFe = (TConsStatServ) context.createUnmarshaller().unmarshal(docConsStatServ);
            ufIBGE = Integer.valueOf(tConsStatServNFe.getCUF());
            versao = tConsStatServNFe.getVersao();

            // Convert to String and unpretty xml
            String xmlFinal = XMLUtils.convertDocumentToString(docConsStatServ);

            // Convert to Document
            Document docFinal = XMLUtils.convertStringToDocument(xmlFinal);
            docFinal.setXmlStandalone(true);

            //Clean document namespace
            XMLUtils.cleanNameSpace(docFinal);

            //Determina o tipo de emissao
            Integer tipoEmissaoAtual = getTipoEmissao(tConsStatServNFe.getCUF());

            NfeDadosMsg nfeDadosMsg = new NfeDadosMsg();
            nfeDadosMsg.getContent().add(docFinal.getDocumentElement());

            String url = getUrl(ufIBGE, tipoEmissaoAtual, ServicosEnum.CONSULTA_STATUS_SERVICO.getNome(), versao);

            return consultarStatusServicoNFeClient.nfeStatusServico(url, nfeDadosMsg, ufIBGE);

        } catch (FazemuServiceUrlNotFoundException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.debug("Erro ao processar ufIBGE " + ufIBGE + " tipoEmissao " + tipoEmissao + " versao " + versao + ": " + e.getMessage());
            return createResultMessage(e, ufIBGE, versao);
        }
    }

    @Override
    public CodigoRetornoAutorizador process(Integer ufIBGE, Integer tipoEmissao, String versao) throws Exception {
        LOGGER.debug("process ufIBGE {} tipoEmissao {} versao {}", ufIBGE, tipoEmissao, versao);
        TConsStatServ consStatServNFe = new TConsStatServ();
        consStatServNFe.setCUF(ufIBGE.toString());
        consStatServNFe.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
        consStatServNFe.setVersao(StringUtils.defaultIfEmpty(versao, ServicosEnum.CONSULTA_STATUS_SERVICO.getVersao()));
        consStatServNFe.setXServ("STATUS");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        LOGGER.debug("process ufIBGE {} versao {} consStatServNFe {}", ufIBGE, consStatServNFe.getVersao(), consStatServNFe);
        context.createMarshaller().marshal(consStatServNFe, document);

        NfeDadosMsg request = new NfeDadosMsg();
        request.getContent().add(document.getDocumentElement());

        //delega para metodo acima
        NfeResultMsg nfeResultMsg = process(request, tipoEmissao);

        List<Object> contentList = nfeResultMsg.getContent();
        TRetConsStatServ retConsStatServNFe = (TRetConsStatServ) context.createUnmarshaller().unmarshal((Node) contentList.get(0));
        Integer cStat = Integer.valueOf(retConsStatServNFe.getCStat());

        CodigoRetornoAutorizador crau = codigoRetornoAutorizadorRepository.findById(cStat);
        if (crau == null) {
            crau = CodigoRetornoAutorizador.build(cStat);
            crau.setTipoDocumentoFiscal(FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE);
            crau.setDescricao(retConsStatServNFe.getXMotivo());
            crau.setSituacaoAutorizador("-");
        } else {
            crau.setDescricao(retConsStatServNFe.getXMotivo()); //prioriza descricao que veio da sefaz e nao da tabela
        }
        return crau;
    }

    protected Document getDocument(NfeDadosMsg nfeDadosMsg) throws JAXBException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        context.createMarshaller().marshal(nfeDadosMsg, document);

        //delete nfeDadosMsg
        Node node = document.getFirstChild().getChildNodes().item(0);
        factory.setNamespaceAware(true);
        Document newDocument = builder.newDocument();
        Node importedNode = newDocument.importNode(node, true);
        newDocument.appendChild(importedNode);

        return newDocument;
    }

    protected NfeResultMsg createResultMessage(Exception exception, Integer ufIBGE, String versao) throws ParserConfigurationException, JAXBException {
        TRetConsStatServ retorno = new TRetConsStatServ();
        retorno.setVersao(versao);
        retorno.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
        retorno.setVerAplic(FazemuUtils.APLICACAO);
        retorno.setCStat(FazemuUtils.ERROR_CODE);
        retorno.setXMotivo(exception.getMessage());
        retorno.setCUF(ufIBGE.toString());
//        retorno.setDhRecbto(null);
//        retorno.setTMed(null);
        retorno.setDhRetorno(DateUtils.newIso8601Date());
//        retorno.setXObs(null);

        Document document = XMLUtils.createNewDocument();
        context.createMarshaller().marshal(retorno, document);

        NfeResultMsg nfeResultMsg = new NfeResultMsg();
        nfeResultMsg.getContent().add(document.getDocumentElement());

        return nfeResultMsg;
    }

}
