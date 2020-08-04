package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.List;

import javax.xml.bind.JAXBContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.b2wdigital.fazemu.business.repository.CodigoRetornoAutorizadorRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.RecepcaoEventoService;
import com.b2wdigital.fazemu.domain.form.CartaCorrecaoForm;
import com.b2wdigital.fazemu.enumeration.CodigoRetornoAutorizadorEnum;
import com.b2wdigital.fazemu.enumeration.RecepcaoEventoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;
import com.b2wdigital.fazemu.utils.DateUtils;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TEvento;
import com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TProcEvento;
import com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TRetEnvEvento;

import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RecepcaoEventoCartaCorrecaoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecepcaoEventoCartaCorrecaoController.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();
    private static final String DEFAULT_MAPPING = "/cartaCorrecao";

    private static final String STR_ZERO = "0";

    @Autowired
    private CodigoRetornoAutorizadorRepository codigoRetornoAutorizadorRepository;
    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;
    @Autowired
    private DocumentoClobRepository documentoClobRepository;
    @Autowired
    private RecepcaoEventoService recepcaoEventoService;
    
    @Autowired
    @Qualifier("nfeRecepcaoEventoCartaCorrecaoContext")
    private JAXBContext contextCartaCorrecao;

    @PostMapping(value = DEFAULT_MAPPING, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public CartaCorrecaoForm cartaCorrecao(@RequestBody CartaCorrecaoForm form) throws Exception {
        LOGGER.info("cartaCorrecao form {}", form);

        String strChaveAcesso = FazemuUtils.normalizarChaveAcesso(form.getChaveAcesso());
        ChaveAcessoNFe chaveAcesso = ChaveAcessoNFe.unparseKey(strChaveAcesso);

        // Define sequencial da carta de correcao
        Integer countCartaCorrecao = documentoClobRepository.countEventosByChaveAcessoAndTipoServico(form.getChaveAcesso(), TipoServicoEnum.CARTA_CORRECAO.getTipoRetorno());
        String nSeqEvento = countCartaCorrecao == null ? "1" : String.valueOf(countCartaCorrecao + 1);

        Integer codigoEvento = RecepcaoEventoEnum.CARTA_CORRECAO.getCodigoEvento();
        String versao = ServicosEnum.RECEPCAO_EVENTO_CARTA_CORRECAO.getVersao();

        TEvento.InfEvento.DetEvento detEvento = new TEvento.InfEvento.DetEvento();
        detEvento.setVersao(versao);
        detEvento.setDescEvento(parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, "SEFAZ_CARTA_CORRECAO_DESC_EVENTO"));
        detEvento.setXCondUso(parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, "SEFAZ_CARTA_CORRECAO_COND_USO"));
        detEvento.setXCorrecao(form.getxCorrecao());

        TEvento.InfEvento infEvento = new TEvento.InfEvento();
        infEvento.setId("ID" + codigoEvento + strChaveAcesso + StringUtils.leftPad(nSeqEvento, 2, STR_ZERO));
        infEvento.setCOrgao(chaveAcesso.cUF);
        infEvento.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
        infEvento.setCNPJ(chaveAcesso.cnpjCpf);
        infEvento.setChNFe(strChaveAcesso);
        infEvento.setDhEvento(DateUtils.newIso8601Date());
        infEvento.setTpEvento(String.valueOf(codigoEvento));
        infEvento.setNSeqEvento(nSeqEvento);
        infEvento.setVerEvento(versao);
        infEvento.setDetEvento(detEvento);

        TEvento evento = new TEvento();
        evento.setInfEvento(infEvento);
        evento.setVersao(versao);

        Document document = XMLUtils.createNewDocument();
        contextCartaCorrecao.createMarshaller().marshal(evento, document);
        
        NfeDadosMsg nfeDadosMsg = new NfeDadosMsg();
        nfeDadosMsg.getContent().add(document.getDocumentElement());

        //delega para servico com a tipagem XML
        NfeResultMsg nfeResultMsg = recepcaoEventoService.process(nfeDadosMsg, form.getUsuario());

        List<Object> contentList = nfeResultMsg.getContent();
        Object content = contentList.get(0);
        TRetEnvEvento retEnvEvento = (TRetEnvEvento) contextCartaCorrecao.createUnmarshaller().unmarshal((Node) content);

        String strCStat = retEnvEvento.getCStat();
        String xMotivo = retEnvEvento.getXMotivo();
        if (NumberUtils.isCreatable(strCStat)) {
            Integer cStat = Integer.valueOf(strCStat);

            String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(cStat);
            if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())) {
                form.setMensagemRetorno(xMotivo);
            } else {
                throw new FazemuServiceException(xMotivo);
            }
        } else {
            if (StringUtils.equals(FazemuUtils.PREFIX + FazemuUtils.ERROR_CODE, strCStat)) {
                throw new FazemuServiceException(xMotivo);
            } else {
                form.setMensagemRetorno(xMotivo);
            }
        }

        return form;
    }
    
    @GetMapping(value = DEFAULT_MAPPING + "/{idDocFiscal}")
    public String getDescricaoByIdDocFiscal(@PathVariable("idDocFiscal")Long idDocFiscal) throws Exception{
    	String xmlCartaCorrecao  = documentoClobRepository.getXmlRetornoByIdDocFiscalAndTipoServico(idDocFiscal, TipoServicoEnum.CARTA_CORRECAO);
    	if(StringUtils.isNotBlank(xmlCartaCorrecao)) {
    		Document docCartaCorreacao = XMLUtils.convertStringToDocument(xmlCartaCorrecao);
    		TProcEvento tProcEvento = (TProcEvento) contextCartaCorrecao.createUnmarshaller().unmarshal(docCartaCorreacao);
    		return  tProcEvento.getEvento().getInfEvento().getDetEvento().getXCorrecao();
    	}else {
    		return "";
    	}
    }
}
