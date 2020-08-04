package com.b2wdigital.fazemu.presentation.web.controller;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.xml.bind.JAXBContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.b2wdigital.fazemu.business.observer.ManifestacaoObserver;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.DocumentoFiscalService;
import com.b2wdigital.fazemu.business.service.RecepcaoEventoService;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.form.ManifestacaoForm;
import com.b2wdigital.fazemu.enumeration.RecepcaoEventoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;
import com.b2wdigital.fazemu.utils.DateUtils;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.TxtReader;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TEvento;
import com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TEvento.InfEvento;
import com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TEvento.InfEvento.DetEvento;
import com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TRetEnvEvento;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg;

@CrossOrigin(origins = {"http://localhost:3000"}, allowedHeaders = "*")
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RecepcaoEventoManifestacaoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecepcaoEventoManifestacaoController.class);
    private static final String DEFAULT_MAPPING = "/manifestacao";

    private static final String STR_ZERO = "0";

    private Locale locale = LocaleContextHolder.getLocale();

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private RecepcaoEventoService recepcaoEventoService;

    @Autowired
    private DocumentoFiscalService documentoFiscalService;

    @Autowired
    @Qualifier("nfeRecepcaoEventoManifestacaoContext")
    private JAXBContext contextManifestacao;

    @PostMapping(value = DEFAULT_MAPPING, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ManifestacaoForm manifestar(@Valid @RequestBody ManifestacaoForm manifestacaoForm) throws Exception {
        LOGGER.info("RecepcaoEventoManifestacaoController: manifestar - manifestacaoForm {}", manifestacaoForm);

        processaManifestacao(manifestacaoForm);
        setAlertColor(manifestacaoForm);
        return manifestacaoForm;
    }

    @PostMapping(value = DEFAULT_MAPPING + "/massivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<ManifestacaoForm> downloadXML(HttpServletResponse response, //url
            @RequestBody MultipartFile file, //form
            @RequestHeader(value = "Content-Transfer-Encoding", required = false) String contentTransferEncoding, //conf
            @RequestParam(value = "usuario", required = false) String usuario,
            @RequestParam(value = "tpEvento", required = false) String tpEvento,
            @RequestParam(value = "justificativa", required = false) String justificativa) throws Exception {

        LOGGER.debug("ManifestacaoController: downloadMassivo");

        ByteArrayInputStream bais = new ByteArrayInputStream(file.getBytes());

        // Monta lista de chaves de acesso baseado no arquivo carregado
        Set<String> listaChavesAcesso = TxtReader.getListaChaveAcesso(bais);

        List<ManifestacaoForm> manifestacaoFormList = criaManifestacaoFormList(listaChavesAcesso, usuario, tpEvento, justificativa);
        manifestacaoFormList.forEach(m -> processaManifestacao(m));

        manifestacaoFormList.forEach(m -> m.setRetorno(Lists.newArrayList(Sets.newHashSet(m.getRetorno()))));

        return manifestacaoFormList;
    }

    private List<ManifestacaoForm> criaManifestacaoFormList(Set<String> listaChavesAcesso, String usuario, String tpEvento, String justificativa) {
        List<ManifestacaoForm> manifestacaoFormList = new ArrayList<>();
        ManifestacaoForm manifestacaoForm;

        for (String chaveAcesso : listaChavesAcesso) {
            manifestacaoForm = new ManifestacaoForm();
            manifestacaoForm.setChaveAcesso(chaveAcesso);
            manifestacaoForm.setUsuario(usuario.toUpperCase());
            manifestacaoForm.setTpEvento(tpEvento);
            manifestacaoForm.setJustificativa(justificativa);

            new ManifestacaoObserver(manifestacaoForm);

            manifestacaoFormList.add(manifestacaoForm);
        }

        return manifestacaoFormList;
    }

    private void processaManifestacao(ManifestacaoForm manifestacaoForm) {
        validaJustificativa(manifestacaoForm);

        if (manifestacaoForm.getRetorno().isEmpty()) {
            TEvento evento = criaEvento(manifestacaoForm);
            NfeDadosMsg nfeDadosMsg = criaNfeDadosMsg(evento, manifestacaoForm);
            enviaReq(nfeDadosMsg, manifestacaoForm);
            setSuccess(manifestacaoForm);
        }

    }

    private void validaJustificativa(ManifestacaoForm manifestacaoForm) {
        if (StringUtils.isNotBlank(manifestacaoForm.getTpEvento())) {
            int codEvento = Integer.parseInt(manifestacaoForm.getTpEvento());
            if (codEvento == RecepcaoEventoEnum.MANIFESTACAO_OPERACAO_NAO_REALIZADA.getCodigoEvento()) {
                if (StringUtils.isBlank(manifestacaoForm.getJustificativa())) {
                    manifestacaoForm.adicionaRetorno(messageSource.getMessage("justificativa.not.empty", null, locale));
                } else if (manifestacaoForm.getJustificativa().length() < 15 || manifestacaoForm.getJustificativa().length() > 255) {
                    manifestacaoForm.adicionaRetorno(messageSource.getMessage("justificativa.wrong.length", null, locale));
                }
            }
        } else {
            manifestacaoForm.adicionaRetorno("Manifestacao sem tipo de evento");
        }
    }

    private TEvento criaEvento(ManifestacaoForm manifestacaoForm) {
        try {
            DocumentoFiscal documentoFiscal = documentoFiscalService.findByChaveAcesso(manifestacaoForm.getChaveAcesso());

            if (documentoFiscal != null) {
                String strChaveAcesso = FazemuUtils.normalizarChaveAcesso(manifestacaoForm.getChaveAcesso());
                ChaveAcessoNFe chaveAcesso = ChaveAcessoNFe.unparseKey(strChaveAcesso);

                int codigoEvento = Integer.parseInt(manifestacaoForm.getTpEvento());
                String nSeqEvento = "1";
                String versao = ServicosEnum.RECEPCAO_EVENTO_MANIFESTACAO.getVersao();

                DetEvento detEvento = new DetEvento();
                detEvento.setVersao(versao);
                detEvento.setDescEvento(getDescricaoEvento(codigoEvento));

                if (RecepcaoEventoEnum.MANIFESTACAO_OPERACAO_NAO_REALIZADA.getCodigoEvento().equals(codigoEvento)) {
                    detEvento.setXJust(manifestacaoForm.getJustificativa());
                }

                InfEvento infEvento = new InfEvento();
                infEvento.setId("ID" + codigoEvento + strChaveAcesso + StringUtils.leftPad(nSeqEvento, 2, STR_ZERO));
                infEvento.setCOrgao(chaveAcesso.cUF);
                infEvento.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
                infEvento.setCNPJ((StringUtils.leftPad(String.valueOf(documentoFiscal.getIdDestinatario()), 14, STR_ZERO)));
                infEvento.setChNFe(strChaveAcesso);
                infEvento.setDhEvento(DateUtils.newIso8601Date());
                infEvento.setTpEvento(String.valueOf(codigoEvento));
                infEvento.setNSeqEvento(nSeqEvento);
                infEvento.setVerEvento(versao);
                infEvento.setDetEvento(detEvento);

                TEvento evento = new TEvento();
                evento.setInfEvento(infEvento);
                evento.setVersao(versao);

                return evento;
            }
            manifestacaoForm.adicionaRetorno(messageSource.getMessage("document.not.found", null, locale));
        } catch (Exception e) {
            LOGGER.error(ExceptionUtils.getRootCauseMessage(e));
            manifestacaoForm.adicionaRetorno(ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }

    private String getDescricaoEvento(Integer codigoEvento) {
        switch (codigoEvento) {
            case 210200:
                return RecepcaoEventoEnum.MANIFESTACAO_CONFIRMACAO_OPERACAO.getDescricao();

            case 210210:
                return RecepcaoEventoEnum.MANIFESTACAO_CIENCIA_OPERACAO.getDescricao();

            case 210220:
                return RecepcaoEventoEnum.MANIFESTACAO_DESCONHECIMENTO_OPERACAO.getDescricao();

            case 210240:
                return RecepcaoEventoEnum.MANIFESTACAO_OPERACAO_NAO_REALIZADA.getDescricao();

            default:
                return "";
        }

    }

    private NfeDadosMsg criaNfeDadosMsg(TEvento evento, ManifestacaoForm manifestacaoForm) {

        try {
            Document document = XMLUtils.createNewDocument();
            contextManifestacao.createMarshaller().marshal(evento, document);

            NfeDadosMsg nfeDadosMsg = new NfeDadosMsg();
            nfeDadosMsg.getContent().add(document.getDocumentElement());
            return nfeDadosMsg;

        } catch (Exception e) {
            LOGGER.error(ExceptionUtils.getRootCauseMessage(e));
            manifestacaoForm.adicionaRetorno(ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    private void enviaReq(NfeDadosMsg nfeDadosMsg, ManifestacaoForm manifestacaoForm) {
        try {
            NfeResultMsg nfeResultMsg = recepcaoEventoService.process(nfeDadosMsg, manifestacaoForm.getUsuario());

            List<Object> contentList = nfeResultMsg.getContent();
            Object content = contentList.get(0);
            TRetEnvEvento retEnvEvento = (TRetEnvEvento) contextManifestacao.createUnmarshaller().unmarshal((Node) content);

            String strCStat = retEnvEvento.getCStat();
            String xMotivo = retEnvEvento.getXMotivo();
            LOGGER.info("retEnvEvento {} CSTAT {} xMotivo {}", retEnvEvento, strCStat, xMotivo);
            manifestacaoForm.getRetorno().add(xMotivo);

        } catch (Exception e) {
            LOGGER.error(ExceptionUtils.getRootCauseMessage(e));
            manifestacaoForm.adicionaRetorno(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void setSuccess(ManifestacaoForm manifestacaoForm) {
        if (manifestacaoForm.getRetorno().get(0).contains("Sucesso")) {
            manifestacaoForm.setSuccess(true);
        } else {
            manifestacaoForm.setSuccess(false);
        }
    }

    private void setAlertColor(ManifestacaoForm manifestacaoForm) {
        if (manifestacaoForm.isSuccess()) {
            manifestacaoForm.getRetorno().add(0, "success");
        } else {
            manifestacaoForm.getRetorno().add(0, "danger");
        }

    }

}
