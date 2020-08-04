package com.b2wdigital.fazemu.presentation.web.controller;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoEventoRepository;
import com.b2wdigital.fazemu.business.repository.EmissorRaizCertificadoDigitalRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.DistribuicaoDocumentosService;
import com.b2wdigital.fazemu.business.service.DocumentoFiscalService;
import com.b2wdigital.fazemu.business.service.StorageService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.EmissorRaizCertificadoDigital;
import com.b2wdigital.fazemu.domain.form.DocumentoFiscalForm;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.form.NfeCompletaOuResumoForm;
import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;
import com.b2wdigital.fazemu.utils.DateUtils;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.distDFeInt_v1.DistDFeInt;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.resNFe_v1.ResNFe;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.retDistDFeInt_v1.RetDistDFeInt;
import com.b2winc.corpserv.message.exception.NotFoundException;

import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresse;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresseResponse;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class DocumentoFiscalController {

    private static final String DEFAULT_MAPPING = "/documentoFiscal";
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentoFiscalController.class);

    private static final String STR_ZERO = "0";

    @Autowired
    private DocumentoFiscalService documentoFiscalService;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private DocumentoEventoRepository documentoEventoRepository;

    @Autowired
    private EmissorRaizCertificadoDigitalRepository emissorRaizCertificadoDigitalRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private DistribuicaoDocumentosService distribuicaoDocumentosService;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    @Qualifier("nfeRetAutorizacaoContext")
    private JAXBContext context;

    @Autowired
    @Qualifier("nfeDistribuicaoDocumentosContext")
    private JAXBContext contextDistribuicao;

    @Autowired
    private MessageSource ms;
    private final Locale locale = LocaleContextHolder.getLocale();

    private final ModelMapper modelMapper = new ModelMapper();

    @GetMapping(value = DEFAULT_MAPPING)
    public List<DocumentoFiscal> listByFiltros(@RequestParam(value = "tipoDocumentoFiscal", required = false) String tipoDocumentoFiscal,
            @RequestParam(value = "idEmissor", required = false) Long idEmissor,
            @RequestParam(value = "idDestinatario", required = false) Long idDestinatario,
            @RequestParam(value = "idEstado", required = false) Long idEstado,
            @RequestParam(value = "idMunicipio", required = false) Long idMunicipio,
            @RequestParam(value = "chaveAcesso", required = false) String chaveAcesso,
            @RequestParam(value = "numeroDocumentoFiscal", required = false) Long numeroDocumentoFiscal,
            @RequestParam(value = "numeroInicialDocumentoFiscal", required = false) Long numeroInicialDocumentoFiscal,
            @RequestParam(value = "numeroFinalDocumentoFiscal", required = false) Long numeroFinalDocumentoFiscal,
            @RequestParam(value = "serieDocumentoFiscal", required = false) Long serieDocumentoFiscal,
            @RequestParam(value = "numeroDocumentoFiscalExterno", required = false) Long numeroDocumentoFiscalExterno,
            @RequestParam(value = "tipoEmissao", required = false) Integer tipoEmissao,
            @RequestParam(value = "situacaoDocumento", required = false) String situacaoDocumento,
            @RequestParam(value = "situacao", required = false) String situacao,
            @RequestParam(value = "dataHoraRegistroInicio", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date dataHoraRegistroInicio,
            @RequestParam(value = "dataHoraRegistroFim", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Date dataHoraRegistroFim,
            @RequestParam(value = "quantidadeRegistros", required = false) Long quantidadeRegistros) {

        return documentoFiscalService.listByFiltros(tipoDocumentoFiscal,
                idEmissor,
                idDestinatario,
                idEstado,
                idMunicipio,
                chaveAcesso,
                numeroDocumentoFiscal,
                numeroInicialDocumentoFiscal,
                numeroFinalDocumentoFiscal,
                serieDocumentoFiscal,
                numeroDocumentoFiscalExterno,
                tipoEmissao,
                situacaoDocumento,
                situacao,
                dataHoraRegistroInicio,
                dataHoraRegistroFim,
                quantidadeRegistros);
    }

    @GetMapping(value = DEFAULT_MAPPING + "/manifestacao")
    public List<DocumentoFiscalForm> listByFiltrosManifestacao(@RequestParam Map<String, String> parameters) {
        List<DocumentoFiscalForm> resultado = new ArrayList<DocumentoFiscalForm>();
        DocumentoFiscalForm dff;
        try {
            List<DocumentoFiscal> listaDf = documentoFiscalService.listByFiltros(parameters);
            for (DocumentoFiscal df : listaDf) {
                dff = modelMapper.map(df, DocumentoFiscalForm.class);
                dff.setDataHoraRegStr(DateUtils.convertDateToString(df.getDataHoraReg()));
                dff.setDataHoraManifestacaoStr(DateUtils.convertDateToString(df.getDataHoraManifestacao()));
                resultado.add(dff);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOGGER.error(ExceptionUtils.getStackTrace(e), e);
        }
        return resultado;
    }

    @RequestMapping(value = "/nfe/{chaveAcesso}", method = RequestMethod.GET)
    public TNfeProc nfe(@PathVariable("chaveAcesso") String chaveAcesso) throws Exception {
        chaveAcesso = FazemuUtils.normalizarChaveAcesso(chaveAcesso);
        DocumentoFiscal documentoFiscal = documentoFiscalService.findByChaveAcesso(chaveAcesso);

        String xmlProcessado = documentoClobRepository.getXmlRetornoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);

        if (xmlProcessado == null) {
            xmlProcessado = documentoClobRepository.getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);
        }

        Document docFinal = XMLUtils.convertStringToDocument(xmlProcessado);
        docFinal.setXmlStandalone(true);

        TNfeProc tNFe = (TNfeProc) context.createUnmarshaller().unmarshal(docFinal);
        return tNFe;
    }

    @RequestMapping(value = "/nfe/completa/resumida/{chaveAcesso}", method = RequestMethod.GET)
    public NfeCompletaOuResumoForm notaCompletaOuResumida(@PathVariable("chaveAcesso") String chaveAcesso) throws Exception {

        NfeCompletaOuResumoForm nfe = new NfeCompletaOuResumoForm();

        chaveAcesso = FazemuUtils.normalizarChaveAcesso(chaveAcesso);
        TNfeProc notaFiscalCompleta = documentoFiscalService.getNotaFiscalCompleta(chaveAcesso, context);

        if (notaFiscalCompleta != null) {
            nfe.setCompleta(notaFiscalCompleta);
            nfe.setTipoNota("completa");
        } else {
            ResNFe notaFiscalResumida = documentoFiscalService.getNotaFiscalResumida(chaveAcesso, contextDistribuicao);
            if (notaFiscalResumida != null) {
                nfe.setResumida(notaFiscalResumida);
                nfe.setTipoNota("resumida");
            } else {
                nfe.setTipoNota("nenhuma");
            }
        }
        return nfe;
    }

    @RequestMapping(value = "/nfe/{chaveAcesso}/xml", method = RequestMethod.GET)
    public byte[] getXmlProcByChaveAcesso(HttpServletResponse response, 
    		@RequestHeader(value = "Content-Transfer-Encoding", required = false) String contentTransferEncoding,
    		@PathVariable("chaveAcesso") String chaveAcesso) throws Exception {
        chaveAcesso = FazemuUtils.normalizarChaveAcesso(chaveAcesso);
        DocumentoFiscal documentoFiscal = documentoFiscalService.findByChaveAcesso(chaveAcesso);

        String xmlProcessado = documentoClobRepository.getXmlRetornoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);

        if (xmlProcessado == null) {
            xmlProcessado = documentoClobRepository.getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);
        }

        if (xmlProcessado == null) {
            xmlProcessado = documentoFiscalService.getXMLResumoNFeByChaveAcesso(chaveAcesso);
        }

        // Busca na AWS S3
        if (xmlProcessado == null) {
            xmlProcessado = storageService.recoverFromStorage(chaveAcesso, TipoServicoEnum.AUTORIZACAO.getTipoRetorno());
        }

        if (xmlProcessado == null) {
            throw new NotFoundException("Chave " + chaveAcesso + " n\u00E3o encontrada.");
        }

        xmlProcessado = XMLUtils.getPrettyXmlFormat(xmlProcessado, 2);
        
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("text/xml");
        
        byte[] xmlBytes = xmlProcessado.getBytes();
        
        try {
            if (StringUtils.equalsIgnoreCase(contentTransferEncoding, "base64")) {
                IOUtils.write(Base64.getEncoder().encode(xmlBytes), response.getOutputStream());
                return Base64.getEncoder().encode(xmlBytes);
            } else {
                IOUtils.write(xmlBytes, response.getOutputStream());
                return xmlBytes;
            }
        } finally {
            response.getOutputStream().flush();
            response.getOutputStream().close();
        }
        
    }

    @RequestMapping(value = "/nfe/{chaveAcesso}/documentoFiscal", method = RequestMethod.GET)
    public DocumentoFiscal findByChaveAcesso(@PathVariable("chaveAcesso") String chaveAcesso) throws Exception {

        chaveAcesso = FazemuUtils.normalizarChaveAcesso(chaveAcesso);
        DocumentoFiscal documentoFiscal = documentoFiscalService.findByChaveAcesso(chaveAcesso);

        if (documentoFiscal == null) {
            throw new NotFoundException("Chave " + chaveAcesso + " n\u00E3o encontrada.");
        }
        return documentoFiscal;
    }

    @GetMapping(value = "/xml-retorno/{idXml}", produces = MediaType.APPLICATION_XML_VALUE)
    public String getXmlById(@PathVariable("idXml") Long idXml) throws Exception {
        DocumentoClob docl = documentoClobRepository.findById(idXml);

        if (docl == null) {
            throw new NotFoundException("ID do XML " + idXml + " n\u00E3o encontrado.");
        }

        return docl.getClob();
    }

    @PostMapping(value = DEFAULT_MAPPING + "/incluirDocumento", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public DocumentoFiscalForm incluirDocumento(HttpServletResponse response, //url
            @RequestBody(required = false) MultipartFile file, //form
            @RequestHeader(value = "Content-Transfer-Encoding", required = false) String contentTransferEncoding, //conf
            @RequestParam(value = "usuario", required = true) String usuario,
            @RequestParam(value = "chaveAcesso", required = true) String chaveAcesso,
            @RequestParam(value = "destinatario", required = true) String destinatario) throws Exception {

        DocumentoFiscalForm documentoFiscalForm = criaForm(chaveAcesso, destinatario, usuario);
        if (documentoFiscalForm.getRetorno().isEmpty()) {
            try {

                Long raizEmissorDestinatario = FazemuUtils.obterRaizCNPJ(Long.valueOf(destinatario));

                EmissorRaizCertificadoDigital emissorRaiz = emissorRaizCertificadoDigitalRepository.findByIdEmissorRaiz(raizEmissorDestinatario);

                if (emissorRaiz == null) {
                    ChaveAcessoNFe chaveAcessoNFe = ChaveAcessoNFe.unparseKey(chaveAcesso);
                    raizEmissorDestinatario = FazemuUtils.obterRaizCNPJ(Long.valueOf(chaveAcessoNFe.cnpjCpf));
                    emissorRaiz = emissorRaizCertificadoDigitalRepository.findByIdEmissorRaiz(raizEmissorDestinatario);
                } else {
                    NfeDistDFeInteresse.NfeDadosMsg xmlEnvio = montaXMLDistribuicaoDFeConsChNFe(chaveAcesso, Long.valueOf(destinatario));
                    RetDistDFeInt retornoEnvio = enviaXml(xmlEnvio);

                    if ("640".equals(retornoEnvio.getCStat())
                            || "FZM999".equals(retornoEnvio.getCStat())) {
                        documentoFiscalForm.getRetorno().add(retornoEnvio.getXMotivo());
                        documentoFiscalForm.setSuccess(false);
                        emissorRaiz = null;
                    }
                }

                if (emissorRaiz == null) {
                    documentoFiscalForm.getRetorno().add("Certificado Digital nao localizado para Emissor/Destinatario.");
                    documentoFiscalForm.setSuccess(false);
                } else {
                    documentoFiscalForm = documentoFiscalService.persist(documentoFiscalForm);

                    if (file != null && documentoFiscalForm.isSuccess()) {
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(file.getBytes());

                        String xml = XMLUtils.getXml(byteArrayInputStream);

                        // unpretty
                        xml = XMLUtils.unPrettyXml(xml);

                        Long idClobXmlRaw = documentoClobRepository.insert(DocumentoClob.build(null, xml, usuario));
                        documentoEventoRepository.insert(DocumentoEvento.build(null, documentoFiscalForm.getIdDocumentoFiscal(), PontoDocumentoEnum.PROCESSADO.getCodigo(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null, idClobXmlRaw, usuario));

                        documentoFiscalForm.getRetorno().add(ms.getMessage("success.add.xml", null, locale));
                        documentoFiscalForm.setSuccess(true);
                    }
                }
            } catch (Exception e) {
                documentoFiscalForm.getRetorno().add(ExceptionUtils.getRootCauseMessage(e));
                documentoFiscalForm.setSuccess(false);
            }
        }
        return documentoFiscalForm;
    }

    private DocumentoFiscalForm criaForm(String chaveAcesso, String destinatario, String usuario) {
        String regex = "[0-9]+";
        try {
            if (destinatario.matches(regex) && destinatario.length() < 15) {
                return new DocumentoFiscalForm(chaveAcesso, destinatario, usuario);
            }
        } catch (Exception e) {
        }
        DocumentoFiscalForm dff = new DocumentoFiscalForm();
        dff.getRetorno().add(ms.getMessage("destinatario.not.valid", null, locale));
        dff.setSuccess(false);
        return dff;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public DocumentoFiscalForm handleMissingParams(MissingServletRequestParameterException ex) {
        DocumentoFiscalForm dff = new DocumentoFiscalForm();
        String parameter = ex.getParameterName();

        if ("chaveAcesso".equals(parameter)) {
            dff.getRetorno().add(ms.getMessage("chaveDeAcesso.not.empty", null, locale));
        }
        if ("destinatario".equals(parameter)) {
            dff.getRetorno().add(ms.getMessage("destinatario.not.empty", null, locale));
        }
        if ("usuario".equals(parameter)) {
            dff.getRetorno().add(ms.getMessage("usuario.not.empty", null, locale));
        }
        dff.setSuccess(false);
        return dff;
    }

    private NfeDistDFeInteresse.NfeDadosMsg montaXMLDistribuicaoDFeConsChNFe(String chaveAcesso, Long idDestinatario) {
        try {
            ChaveAcessoNFe chaveAcessoNFe = ChaveAcessoNFe.unparseKey(chaveAcesso);

            DistDFeInt.ConsChNFe consChNFe = new DistDFeInt.ConsChNFe();
            consChNFe.setChNFe(chaveAcesso);

            DistDFeInt distDFeInt = new DistDFeInt();
            distDFeInt.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
            distDFeInt.setCUFAutor(chaveAcessoNFe.cUF);

            distDFeInt.setCNPJ(StringUtils.leftPad(String.valueOf(idDestinatario), 14, STR_ZERO));
            distDFeInt.setConsChNFe(consChNFe);
            distDFeInt.setVersao(ServicosEnum.DISTRIBUICAO_DFE.getVersao());

            Document document = XMLUtils.createNewDocument();
            contextDistribuicao.createMarshaller().marshal(distDFeInt, document);

            NfeDistDFeInteresse.NfeDadosMsg nfeDadosMsg = new NfeDistDFeInteresse.NfeDadosMsg();
            nfeDadosMsg.getContent().add(document.getDocumentElement());

            return nfeDadosMsg;

        } catch (Exception e) {
            LOGGER.error("downloadXMLManifestado (scheduled) - montaXMLDistribuicaoDFeConsChNFe {} ", ExceptionUtils.getStackTrace(e), e);
            return null;
        }
    }

    private RetDistDFeInt enviaXml(NfeDistDFeInteresse.NfeDadosMsg nfeDadosMsg) throws Exception {
        NfeDistDFeInteresseResponse response = distribuicaoDocumentosService.process(nfeDadosMsg, 0);
        NfeDistDFeInteresseResponse.NfeDistDFeInteresseResult result = response.getNfeDistDFeInteresseResult();

        List<Object> contentList = result.getContent();
        Object content = contentList.get(0);
        return (RetDistDFeInt) contextDistribuicao.createUnmarshaller().unmarshal((Node) content);
    }

}
