package com.b2wdigital.fazemu.presentation.web.controller;

import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg;
import com.b2wdigital.fazemu.business.repository.CodigoRetornoAutorizadorRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2wdigital.fazemu.business.service.RecepcaoEventoService;
import com.b2wdigital.fazemu.domain.form.CancelamentoForm;
import com.b2wdigital.fazemu.enumeration.CodigoRetornoAutorizadorEnum;
import com.b2wdigital.fazemu.enumeration.RecepcaoEventoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;
import com.b2wdigital.fazemu.utils.DateUtils;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
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
public class RecepcaoEventoCancelamentoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecepcaoEventoCancelamentoController.class);
    private static final String DEFAULT_MAPPING = "/cancelamento";

    private static final String STR_ZERO = "0";

    @Autowired
    private CodigoRetornoAutorizadorRepository codigoRetornoAutorizadorRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private RecepcaoEventoService recepcaoEventoService;

    @Autowired
    @Qualifier("nfeRecepcaoEventoCancelamentoContext")
    private JAXBContext jaxbContext;

    @PostMapping(value = DEFAULT_MAPPING, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public CancelamentoForm cancelamento(@RequestBody CancelamentoForm form) throws Exception {
        LOGGER.info("cancelamento form {}", form);

        String strChaveAcesso = FazemuUtils.normalizarChaveAcesso(form.getChaveAcesso());
        ChaveAcessoNFe chaveAcesso = ChaveAcessoNFe.unparseKey(strChaveAcesso);

        Integer codigoEvento = RecepcaoEventoEnum.CANCELAMENTO.getCodigoEvento();
        String nSeqEvento = "1";
        String versao = ServicosEnum.RECEPCAO_EVENTO_CANCELAMENTO.getVersao();

        com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TEvento.InfEvento.DetEvento detEvento = new com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TEvento.InfEvento.DetEvento();
        detEvento.setVersao(versao);
        detEvento.setDescEvento(RecepcaoEventoEnum.CANCELAMENTO.getDescricao());
        detEvento.setNProt(form.getNumeroProtocolo());
        detEvento.setXJust(form.getJustificativa());

        com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TEvento.InfEvento infEvento = new com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TEvento.InfEvento();
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

        com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TEvento evento = new com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TEvento();
        evento.setInfEvento(infEvento);
        evento.setVersao(versao);

        Document document = XMLUtils.createNewDocument();
        jaxbContext.createMarshaller().marshal(evento, document);
        NfeDadosMsg nfeDadosMsg = new NfeDadosMsg();
        nfeDadosMsg.getContent().add(document.getDocumentElement());

        //delega para servico com a tipagem XML
        NfeResultMsg nfeResultMsg = recepcaoEventoService.process(nfeDadosMsg, form.getUsuario());

        LOGGER.info("nfeResultMsg {}", nfeDadosMsg);
        List<Object> contentList = nfeResultMsg.getContent();
        Object content = contentList.get(0);
        LOGGER.info("CONTENT LIST {} CONTENT {} CONTENT CLASS {}", contentList, content, content.getClass());
        com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TRetEnvEvento retEnvEvento = (com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TRetEnvEvento) jaxbContext.createUnmarshaller().unmarshal((Node) content);

        String strCStat = retEnvEvento.getCStat();
        String xMotivo = retEnvEvento.getXMotivo();
        if (NumberUtils.isCreatable(strCStat)) {
            Integer cStat = Integer.valueOf(strCStat);
            LOGGER.info("retEnvEvento {} CSTAT {} xMotivo {}", retEnvEvento, strCStat, xMotivo);

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
}
