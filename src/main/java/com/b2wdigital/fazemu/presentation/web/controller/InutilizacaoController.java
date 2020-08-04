package com.b2wdigital.fazemu.presentation.web.controller;

import br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeResultMsg;
import com.b2wdigital.fazemu.business.repository.CodigoRetornoAutorizadorRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2wdigital.fazemu.business.service.InutilizacaoNFeService;
import com.b2wdigital.fazemu.domain.form.InutilizacaoForm;
import com.b2wdigital.fazemu.enumeration.CodigoRetornoAutorizadorEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.inutNFe_v4.TInutNFe;
import com.b2wdigital.nfe.schema.v4v160b.inutNFe_v4.TRetInutNFe;
import javax.xml.bind.JAXBContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class InutilizacaoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InutilizacaoController.class);

    private static final String PREFIX_ID = "ID";
    private static final String XSERV = "INUTILIZAR";
    private static final String STR_ZERO = "0";
    private static final String DEFAULT_MAPPING = "/inutilizacao";

    @Autowired
    private CodigoRetornoAutorizadorRepository codigoRetornoAutorizadorRepository;
    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;
    @Autowired
    @Qualifier("nfeInutilizacaoContext")
    private JAXBContext jaxbContext;
    @Autowired
    private InutilizacaoNFeService inutilizacaoNFeService;

    @PostMapping(value = DEFAULT_MAPPING, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public InutilizacaoForm inutilizacao(@RequestBody InutilizacaoForm form) throws Exception {
        LOGGER.info("inutilizacao form {}", form);

        if (StringUtils.isNotBlank(form.getChaveAcesso())) {
            String strChaveAcesso = FazemuUtils.normalizarChaveAcesso(form.getChaveAcesso());
            ChaveAcessoNFe chaveAcesso = ChaveAcessoNFe.unparseKey(strChaveAcesso);

            form.setCodigoIbge(Integer.valueOf(chaveAcesso.cUF));
            form.setAno(Integer.valueOf(chaveAcesso.dataAAMM.substring(0, 2)));
            form.setIdEmissor(Long.valueOf(chaveAcesso.cnpjCpf));
            form.setModelo(Integer.valueOf(chaveAcesso.mod));
            form.setSerieDocumentoFiscal(Long.valueOf(chaveAcesso.serie));
            form.setNumeroNFInicial(Long.valueOf(chaveAcesso.nNF));
            form.setNumeroNFFinal(form.getNumeroNFInicial()); //final igual ao inicial
        }

        String versao = ServicosEnum.INUTILIZACAO.getVersao();
        String id = montarIdInutilizacao(form);
        LOGGER.info("id inutilizacao {} len {}", id, id.length());

        TInutNFe.InfInut infInut = new TInutNFe.InfInut();
        infInut.setId(id);
        infInut.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
        infInut.setXServ(XSERV);
        infInut.setCUF(String.valueOf(form.getCodigoIbge()));
        infInut.setAno(String.valueOf(form.getAno() % 100));
        infInut.setCNPJ(StringUtils.leftPad(String.valueOf(form.getIdEmissor()), 14, STR_ZERO));
        infInut.setMod(String.valueOf(form.getModelo()));
        infInut.setSerie(String.valueOf(form.getSerieDocumentoFiscal()));
        infInut.setNNFIni(String.valueOf(form.getNumeroNFInicial()));
        infInut.setNNFFin(String.valueOf(form.getNumeroNFFinal()));
        infInut.setXJust(form.getJustificativa());

        TInutNFe inutNFe = new TInutNFe();
        inutNFe.setInfInut(infInut);
        inutNFe.setVersao(versao);

        Document document = XMLUtils.createNewDocument();
        jaxbContext.createMarshaller().marshal(inutNFe, document);
        NfeDadosMsg nfeDadosMsg = new NfeDadosMsg();
        nfeDadosMsg.getContent().add(document.getDocumentElement());

        //delega para servico com a tipagem XML
        NfeResultMsg nfeResultMsg = inutilizacaoNFeService.process(nfeDadosMsg, form.getUsuario());

        LOGGER.info("nfeResultMsg {}", nfeDadosMsg);
        List<Object> contentList = nfeResultMsg.getContent();
        Object content = contentList.get(0);
        LOGGER.info("CONTENT LIST {} CONTENT {} CONTENT CLASS {}", contentList, content, content.getClass());
        TRetInutNFe retInutNFe = (TRetInutNFe) jaxbContext.createUnmarshaller().unmarshal((Node) content);

        String strCStat = retInutNFe.getInfInut().getCStat();
        String xMotivo = retInutNFe.getInfInut().getXMotivo();
        if (NumberUtils.isCreatable(strCStat)) {
            Integer cStat = Integer.valueOf(strCStat);
            LOGGER.info("retInutNFe {} CSTAT {} xMotivo", retInutNFe, strCStat, xMotivo);

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

    protected String montarIdInutilizacao(InutilizacaoForm form) {
        return montarIdInutilizacao(form.getCodigoIbge(), form.getAno(), form.getIdEmissor(), form.getModelo(), form.getSerieDocumentoFiscal(), form.getNumeroNFInicial(), form.getNumeroNFFinal());
    }

    protected String montarIdInutilizacao(int codigoIbge, int ano, Long idEmissor, int modelo, long serie, long nIni, long nFim) {
        return PREFIX_ID
                + codigoIbge
                + (ano % 100)
                + StringUtils.leftPad(String.valueOf(idEmissor), 14, STR_ZERO)
                + StringUtils.leftPad(String.valueOf(modelo), 2, STR_ZERO)
                + StringUtils.leftPad(String.valueOf(serie), 3, STR_ZERO)
                + StringUtils.leftPad(String.valueOf(nIni), 9, STR_ZERO)
                + StringUtils.leftPad(String.valueOf(nFim), 9, STR_ZERO);
    }
}
