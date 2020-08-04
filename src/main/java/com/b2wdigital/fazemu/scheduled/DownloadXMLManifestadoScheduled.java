package com.b2wdigital.fazemu.scheduled;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.DistribuicaoDocumentosService;
import com.b2wdigital.fazemu.business.service.DocumentoClobService;
import com.b2wdigital.fazemu.business.service.DocumentoEventoService;
import com.b2wdigital.fazemu.business.service.DocumentoFiscalService;
import com.b2wdigital.fazemu.business.service.DocumentoRetornoService;
import com.b2wdigital.fazemu.business.service.EstadoService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.DocumentoRetorno;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.FazemuScheduledException;
import com.b2wdigital.fazemu.form.DocumentoInfo;
import com.b2wdigital.fazemu.integration.dao.redis.RedisOperationsDao;
import com.b2wdigital.fazemu.service.impl.AbstractNFeServiceImpl;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.LoteUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.distDFeInt_v1.DistDFeInt;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.distDFeInt_v1.DistDFeInt.ConsChNFe;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.retDistDFeInt_v1.RetDistDFeInt;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.retDistDFeInt_v1.RetDistDFeInt.LoteDistDFeInt.DocZip;
import com.google.common.collect.Lists;

import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresse.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresseResponse;

@Component
public class DownloadXMLManifestadoScheduled extends AbstractNFeServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadXMLManifestadoScheduled.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    private static final String KEY = RedisOperationsDao.KEY_SEMAFORO_DOWNLOAD_XML_MANIFESTADO;
    private static final String LOCK_HOST_EM_PROCESSAMENTO = "LOCK_DOWNLOAD_XML_MANIFESTADO";
    private static final String LOCK_EM_SELECAO_DE_DOCUMENTOS = "LOCK_DOWNLOAD_XML_MANIFESTADO_EM_SELECAO_DE_DOCUMENTOS";

    private static final String STR_ZERO = "0";

    @Autowired
    private DistribuicaoDocumentosService distribuicaoDocumentosService;

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Autowired
    private EstadoService estadoService;

    @Autowired
    private DocumentoFiscalService documentoFiscalService;

    @Autowired
    private DocumentoClobService documentoClobService;

    @Autowired
    private DocumentoEventoService documentoEventoService;

    @Autowired
    private DocumentoRetornoService documentoRetornoService;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    @Qualifier("nfeDistribuicaoDocumentosContext")
    private JAXBContext contextDistribuicao;

    @Autowired
    @Qualifier("nfeRetAutorizacaoContext")
    private JAXBContext contextAutorizacao;

    private String usuario;

    @Scheduled(fixedDelay = 1200_000L) //20 em 20 minutos 
    public void downloadXMLManifestado() throws Exception {
        LOGGER.info("downloadXMLManifestado (scheduled) - INICIO");

        String statusScheduled = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_SCHEDULED_DOWNLOAD_XML_MANIFESTADO);

        if (!"ON".equals(statusScheduled)) {
            throw new FazemuScheduledException("Chave de robo desligada");
        }
        usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

        String myHostLock = LoteUtils.getMyHost() + "_" + LOCK_HOST_EM_PROCESSAMENTO;

        if (cacheLoteRepository.setIfAbsent(myHostLock, true, 3, TimeUnit.MINUTES)) {
            List<DocumentoRetorno> listaDocumentosEleitos = getDocumentosElegiveis();

            if (listaDocumentosEleitos.size() > 0) {
                LOGGER.info("downloadXMLManifestado (scheduled) - listaDocumentosEleitos {}", listaDocumentosEleitos);

                try {
                    listaDocumentosEleitos.forEach((documentoRetorno) -> {
                        try {
                            DocumentoFiscal documentoFiscal = documentoFiscalService.findById(documentoRetorno.getIdDocumentoFiscal());
                            LOGGER.info("DownloadXMLManifestado (scheduled) - {} - Documento Fiscal", documentoFiscal.getId());

                            if (documentoFiscal != null) {
                                NfeDadosMsg xmlEnvio = montaXMLDistribuicaoDFeConsChNFe(documentoFiscal);
                                RetDistDFeInt retornoEnvio = enviaXml(xmlEnvio);

                                if ("656".equals(retornoEnvio.getCStat())) {
                                    LOGGER.info("DownloadXMLManifestado (scheduled) - {} - Consumo Indevido", documentoFiscal.getIdDestinatario());
                                } else if ("138".equals(retornoEnvio.getCStat())) {
                                    List<DocZip> listaDocZipProcNFe = filtraZipsPorProcNFe(retornoEnvio.getLoteDistDFeInt().getDocZip());
                                    List<DocumentoInfo> listaDocumentoInfo = zipToDocumentoInfo(listaDocZipProcNFe);

                                    prepareAndPersist(listaDocumentoInfo);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("downloadXMLManifestado (scheduled) - {} - ERROR: {}", documentoRetorno.getIdDocumentoFiscal(), e);
                        }
                    });

                } catch (Exception e) {
                    LOGGER.error("Erro ao criar callableList", e);
                }
            }

            // Excluir os eleitos do semaforo
            removerMembrosLista(listaDocumentosEleitos);

            redisOperationsService.expiresKey(myHostLock, 1L, TimeUnit.MILLISECONDS);

        }

        LOGGER.info("downloadXMLManifestado (scheduled) - FIM");
    }

    private List<DocZip> filtraZipsPorProcNFe(List<DocZip> docZipList) {
        if (docZipList != null) {
            List<DocZip> listaDocZipProcNFe = new ArrayList<>();

            docZipList.forEach((zip) -> {
                String schema = zip.getSchema();
                if (schema != null) {
                    if (schema.contains("procNFe")) {
                        listaDocZipProcNFe.add(zip);
                    }
                }
            });

            return listaDocZipProcNFe;
        }
        return null;
    }

    private List<DocumentoInfo> zipToDocumentoInfo(List<DocZip> listaDocZipProcNFe) throws Exception {
        List<DocumentoInfo> procNFeDocuments = new ArrayList<>();

        String conteudoDecode;
        Document documento;
        String ultimoNSU;
        DocumentoInfo docInf;

        for (DocZip zip : listaDocZipProcNFe) {
            conteudoDecode = XMLUtils.convertGZipToXml(zip.getValue());
            documento = XMLUtils.convertStringToDocument(conteudoDecode);
            ultimoNSU = zip.getNSU();

            docInf = new DocumentoInfo(conteudoDecode, documento, ultimoNSU);
            procNFeDocuments.add(docInf);
        }
        return procNFeDocuments;
    }

    private void prepareAndPersist(List<DocumentoInfo> listaDocumentoInfo) throws Exception {
        if (!listaDocumentoInfo.isEmpty()) {
            for (DocumentoInfo docInfo : listaDocumentoInfo) {
                TNfeProc nfeProc = (TNfeProc) contextAutorizacao.createUnmarshaller().unmarshal(docInfo.getDoc());
                docInfo.setTnfeProc(nfeProc);
            }
            persist(listaDocumentoInfo);
        }

    }

    private void persist(List<DocumentoInfo> docInfoList) {
        for (DocumentoInfo docInfo : docInfoList) {
            if (docInfo.getTnfeProc() != null) {
                TNfeProc tNfeProc = docInfo.getTnfeProc();
                String chaveAcesso = tNfeProc.getProtNFe().getInfProt().getChNFe();

                DocumentoFiscal docu = documentoFiscalService.findByChaveAcesso(chaveAcesso);
                Long idXmlClob = documentoClobService.insert(DocumentoClob.build(null, docInfo.getConteudoDecode(), usuario));
                documentoEventoService.insert(DocumentoEvento.build(null, docu.getId(), PontoDocumentoEnum.PROCESSADO.getCodigo(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), tNfeProc.getProtNFe().getInfProt().getCStat(), idXmlClob, usuario));
                documentoRetornoService.insert(docu.getId(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null, idXmlClob, usuario, usuario);

            }
        }

    }

    private NfeDadosMsg montaXMLDistribuicaoDFeConsChNFe(DocumentoFiscal documentoFiscal) {
        try {
            ConsChNFe consChNFe = new ConsChNFe();
            consChNFe.setChNFe(documentoFiscal.getChaveAcessoEnviada());

            DistDFeInt distDFeInt = new DistDFeInt();
            distDFeInt.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
            distDFeInt.setCUFAutor(estadoService.findById(documentoFiscal.getIdEstado()).getCodigoIbge().toString());

            distDFeInt.setCNPJ(StringUtils.leftPad(String.valueOf(documentoFiscal.getIdDestinatario()), 14, STR_ZERO));
            distDFeInt.setConsChNFe(consChNFe);
            distDFeInt.setVersao(ServicosEnum.DISTRIBUICAO_DFE.getVersao());

            Document document = XMLUtils.createNewDocument();
            contextDistribuicao.createMarshaller().marshal(distDFeInt, document);

            NfeDadosMsg nfeDadosMsg = new NfeDadosMsg();
            nfeDadosMsg.getContent().add(document.getDocumentElement());

            return nfeDadosMsg;

        } catch (Exception e) {
            LOGGER.error("downloadXMLManifestado (scheduled) - montaXMLDistribuicaoDFeConsChNFe {} ", ExceptionUtils.getStackTrace(e), e);
            return null;
        }
    }

    private RetDistDFeInt enviaXml(NfeDadosMsg nfeDadosMsg) throws Exception {
        NfeDistDFeInteresseResponse response = distribuicaoDocumentosService.process(nfeDadosMsg, 0);
        NfeDistDFeInteresseResponse.NfeDistDFeInteresseResult result = response.getNfeDistDFeInteresseResult();

        List<Object> contentList = result.getContent();
        Object content = contentList.get(0);
        return (RetDistDFeInt) contextDistribuicao.createUnmarshaller().unmarshal((Node) content);
    }

    private List<DocumentoRetorno> getDocumentosElegiveis() {

        List<DocumentoRetorno> documentosElegiveis = Lists.newArrayList();

        if (cacheLoteRepository.setIfAbsent(LOCK_EM_SELECAO_DE_DOCUMENTOS, true, 1, TimeUnit.MINUTES)) {

            try {
                Set<Object> documentosEmProcessamento = redisOperationsService.members(KEY);

                String notIn = null;
                if (CollectionUtils.isNotEmpty(documentosEmProcessamento)) {
                    notIn = documentosEmProcessamento.stream().map(idDocFiscal -> String.valueOf(idDocFiscal)).collect(Collectors.joining(","));
                }

                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MINUTE, -parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_MIN_DOWNLOAD_XML_MANIFESTADO, 10));

                List<DocumentoRetorno> documentosAProcessar = documentoRetornoService.listByDataHoraInicioAndNotExistsXML(cal.getTime(), notIn);
                if (CollectionUtils.isNotEmpty(documentosAProcessar)) {

                    // Limita o processo a quantidade pre estabelecida
                    int count = 0;
                    Integer docsPorProcesso = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_DOCUMENTOS_PROCESSADOS_EM_PARALELO, 30);

                    for (DocumentoRetorno documentoRetorno : documentosAProcessar) {
                        documentosElegiveis.add(documentoRetorno);
                        redisOperationsService.addToSet(KEY, documentoRetorno.getIdDocumentoFiscal());
                        count++;
                        if (count == docsPorProcesso) {
                            break;
                        }
                    }

                    if (CollectionUtils.isNotEmpty(documentosElegiveis)) {
                        redisOperationsService.expiresKey(KEY, 5L, TimeUnit.MINUTES);
                    }
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        redisOperationsService.expiresKey(LOCK_EM_SELECAO_DE_DOCUMENTOS, 1L, TimeUnit.MILLISECONDS);
        return documentosElegiveis;
    }

    private void removerMembrosLista(List<DocumentoRetorno> lista) {
        LOGGER.info("downloadXMLManifestado (scheduled) - removerMembrosLista {}", lista.size());

        lista.forEach((documento) -> {
            redisOperationsService.removeFromSet(KEY, documento.getIdDocumentoFiscal());
        });
    }

}
