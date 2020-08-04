package com.b2wdigital.fazemu.scheduled;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;

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
import com.b2wdigital.fazemu.business.repository.CodigoRetornoAutorizadorRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoEventoRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.DistribuicaoDocumentosService;
import com.b2wdigital.fazemu.business.service.EmissorRaizFilialService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.EmissorRaizFilial;
import com.b2wdigital.fazemu.enumeration.CodigoRetornoAutorizadorEnum;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;
import com.b2wdigital.fazemu.exception.FazemuScheduledException;
import com.b2wdigital.fazemu.form.DocumentoInfo;
import com.b2wdigital.fazemu.service.impl.AbstractNFeServiceImpl;
import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;
import com.b2wdigital.fazemu.utils.DateUtils;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.LoteUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.distDFeInt_v1.DistDFeInt;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.distDFeInt_v1.DistDFeInt.DistNSU;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.resNFe_v1.ResNFe;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.retDistDFeInt_v1.RetDistDFeInt;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.retDistDFeInt_v1.RetDistDFeInt.LoteDistDFeInt.DocZip;

import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresse.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresseResponse;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import java.util.Date;

@Component
public class DistribuicaoDocumentosScheduled extends AbstractNFeServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistribuicaoDocumentosScheduled.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();
    private static final String LOCK_HOST_EM_PROCESSAMENTO = "LOCK_CONSULTAR_DISTRIBUICAO_DOCUMENTOS";
    private static final String LOCK_EM_SELECAO_DE_FILIAIS = "LOCK_CONSULTAR_DISTRIBUICAO_DOCUMENTOS_EM_SELECAO_DE_FILIAIS";

    private static final String STR_ZERO = "0";

    @Autowired
    private DistribuicaoDocumentosService distribuicaoDocumentosService;

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Autowired
    private EmissorRaizFilialService emissorRaizFilialService;

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private DocumentoEventoRepository documentoEventoRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private CodigoRetornoAutorizadorRepository codigoRetornoAutorizadorRepository;

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    @Qualifier("nfeDistribuicaoDocumentosContext")
    private JAXBContext contextDistribuicao;

    @Autowired
    @Qualifier("nfeRetAutorizacaoContext")
    private JAXBContext contextAutorizacao;

    private String usuario;

    @Scheduled(fixedDelay = 1800_000L) //30 em 30 minutos 
    public void distribuirDocumentos() throws Exception {
        LOGGER.info("distribuirDocumentos (scheduled) - INICIO");

        String statusScheduled = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_SCHEDULED_DISTRIBUICAO_DOCUMENTOS);

        if (!"ON".equals(statusScheduled)) {
            throw new FazemuScheduledException("Chave de robo desligada");
        }
        usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

        String myHostLock = LoteUtils.getMyHost() + "_" + LOCK_HOST_EM_PROCESSAMENTO;

        if (cacheLoteRepository.setIfAbsent(myHostLock, true, 3, TimeUnit.MINUTES)) {

            List<EmissorRaizFilial> filiais = listaFiliais();
            for (EmissorRaizFilial emissorRaizFilial : filiais) {
                try {
                    Long ultimoNSUTabela;
                    Long ultimoNSURequisicao;
                    Long maxNSURequisicao;

                    if (cacheLoteRepository.setIfAbsent(LOCK_EM_SELECAO_DE_FILIAIS, true, 1, TimeUnit.MINUTES)) {
                        NfeDadosMsg xmlEnvio = montaXMLDistribuicaoDFeDistNSU(emissorRaizFilial);
                        RetDistDFeInt retornoEnvio = enviaXml(xmlEnvio);

                        if ("656".equals(retornoEnvio.getCStat())) {
                            LOGGER.info("distribuirDocumentos (scheduled) - {} - Consumo Indevido", emissorRaizFilial.getIdFilial());
                            break;
                        }

                        ultimoNSUTabela = Long.valueOf(emissorRaizFilial.getUltimoNSU());
                        ultimoNSURequisicao = Long.valueOf(retornoEnvio.getUltNSU());
                        maxNSURequisicao = Long.valueOf(retornoEnvio.getMaxNSU());

                        if ("138".equals(retornoEnvio.getCStat())) {
                            if (!ultimoNSUTabela.equals(ultimoNSURequisicao)
                                    || (ultimoNSUTabela.equals(ultimoNSURequisicao)
                                    && !ultimoNSUTabela.equals(maxNSURequisicao))) {
                                Map<String, List<DocZip>> zipsFiltrados = filtraZipsPorResNfeOuProcNfe(retornoEnvio.getLoteDistDFeInt().getDocZip());
                                Map<String, List<DocumentoInfo>> documentsInfoMap = zipToDocumentoInfo(zipsFiltrados);
                                List<DocumentoInfo> documentoFinalListResNFe = documentsInfoMap.get("resNFe");
                                List<DocumentoInfo> documentoFinalListTNFeProc = documentsInfoMap.get("procNFe");

                                prepareAndPersist(documentoFinalListResNFe, documentoFinalListTNFeProc, emissorRaizFilial);
                            }
                        } else if ("137".equals(retornoEnvio.getCStat())) {
                            Long ultimoNSU;
                            if (ultimoNSURequisicao > ultimoNSUTabela + 50) {
                                ultimoNSU = ultimoNSUTabela + 30;
                            } else {
                                ultimoNSU = ultimoNSURequisicao;
                            }
                            emissorRaizFilialService.updateUltimoNSU(emissorRaizFilial.getIdFilial(), StringUtils.leftPad(ultimoNSU.toString(), 15, STR_ZERO), usuario);
                        }
                    }
                    redisOperationsService.expiresKey(LOCK_EM_SELECAO_DE_FILIAIS, 1L, TimeUnit.MILLISECONDS);
                    LOGGER.info("distribuirDocumentos (scheduled) - {} - FIM", emissorRaizFilial.getIdFilial());

                } catch (Exception e) {
                    LOGGER.error("distribuirDocumentos (scheduled) - {} - ERROR: {}", emissorRaizFilial.getIdFilial(), e);
                }
            }
            redisOperationsService.expiresKey(myHostLock, 1L, TimeUnit.MILLISECONDS);

        }

        LOGGER.info("distribuirDocumentos (scheduled) - FIM");
    }

    private Map<String, List<DocZip>> filtraZipsPorResNfeOuProcNfe(List<DocZip> docZipList) {
        if (docZipList != null) {
            Map<String, List<DocZip>> mapa = new HashMap<>();
            List<DocZip> zipsValidosResNFe = new ArrayList<>();
            List<DocZip> zipsValidosProcNFe = new ArrayList<>();

            docZipList.forEach((zip) -> {
                String schema = zip.getSchema();
                if (schema != null) {
                    if (schema.contains("resNFe")) {
                        zipsValidosResNFe.add(zip);
                    }
                    //Serviço de Download de XML Completo ativo desabilitado, utilizando downloadXMLManifestadoScheduled
                    //if (schema.contains("procNFe")) {
                    //    zipsValidosProcNFe.add(zip);
                    //}
                }
            });

            mapa.put("resNFe", zipsValidosResNFe);
            mapa.put("procNFe", zipsValidosProcNFe);
            return mapa;
        }
        return null;
    }

    private Map<String, List<DocumentoInfo>> zipToDocumentoInfo(Map<String, List<DocZip>> filteredZips) throws Exception {

        Map<String, List<DocumentoInfo>> documentsMap = new HashMap<>();
        List<DocumentoInfo> resNFeDocuments = new ArrayList<>();
        List<DocumentoInfo> procNFeDocuments = new ArrayList<>();

        String conteudoDecode;
        Document documento;
        String ultimoNSU;
        DocumentoInfo docInf;

        for (Map.Entry<String, List<DocZip>> entry : filteredZips.entrySet()) {
            for (DocZip zip : entry.getValue()) {
                conteudoDecode = XMLUtils.convertGZipToXml(zip.getValue());
                documento = XMLUtils.convertStringToDocument(conteudoDecode);
                ultimoNSU = zip.getNSU();

                docInf = new DocumentoInfo(conteudoDecode, documento, ultimoNSU);

                if (entry.getKey().equals("resNFe")) {
                    resNFeDocuments.add(docInf);
                } else if (entry.getKey().equals("procNFe")) {
                    procNFeDocuments.add(docInf);
                }
            }
        }

        documentsMap.put("resNFe", resNFeDocuments);
        documentsMap.put("procNFe", procNFeDocuments);
        return documentsMap;
    }

    private void prepareAndPersist(List<DocumentoInfo> documentoFinalListResNFe, List<DocumentoInfo> documentoFinalListTNFeProc, EmissorRaizFilial emissorRaizFilial) throws Exception {
        if (!documentoFinalListResNFe.isEmpty()) {
            for (DocumentoInfo docInfo : documentoFinalListResNFe) {
                ResNFe resNFe = (ResNFe) contextDistribuicao.createUnmarshaller().unmarshal(docInfo.getDoc());
                docInfo.setResNFe(resNFe);
            }
            persist(documentoFinalListResNFe, emissorRaizFilial);
        }
        if (!documentoFinalListTNFeProc.isEmpty()) {
            for (DocumentoInfo docInfo : documentoFinalListTNFeProc) {
                TNfeProc nfeProc = (TNfeProc) contextAutorizacao.createUnmarshaller().unmarshal(docInfo.getDoc());
                docInfo.setTnfeProc(nfeProc);
            }
            persist(documentoFinalListTNFeProc, emissorRaizFilial);
        }
        if (documentoFinalListResNFe.isEmpty() && documentoFinalListTNFeProc.isEmpty()) {
            Long ultimoNSU = Long.valueOf(emissorRaizFilial.getUltimoNSU()) + 40;
            emissorRaizFilialService.updateUltimoNSU(emissorRaizFilial.getIdFilial(), StringUtils.leftPad(ultimoNSU.toString(), 15, STR_ZERO), usuario);
        }

    }

    private void persist(List<DocumentoInfo> docInfoList, EmissorRaizFilial emissorRaizFilial) throws Exception {
        for (DocumentoInfo docInfo : docInfoList) {
            String chaveAcesso = null;
            Long idEmissor = null;
            Date dataHoraEmissao = null;
            String versao = null;
            String cStat = null;
            String situacaoDocumento = null;
            String pontoDocumento = null;
            String pontoEvento = null;
            String tipoServico = null;
            if (docInfo.getTnfeProc() != null) {
                TNfeProc tNfeProc = docInfo.getTnfeProc();

                chaveAcesso = tNfeProc.getProtNFe().getInfProt().getChNFe();
                idEmissor = tNfeProc.getNFe().getInfNFe().getEmit().getCNPJ() == null ? Long.valueOf(tNfeProc.getNFe().getInfNFe().getEmit().getCPF()) : Long.valueOf(tNfeProc.getNFe().getInfNFe().getEmit().getCNPJ());
                dataHoraEmissao = DateUtils.iso8601ToCalendar(tNfeProc.getNFe().getInfNFe().getIde().getDhEmi()).getTime();
                versao = tNfeProc.getProtNFe().getVersao();
                cStat = tNfeProc.getProtNFe().getInfProt().getCStat();
                pontoDocumento = PontoDocumentoEnum.PROCESSADO.getCodigo();
                pontoEvento = PontoDocumentoEnum.PROCESSADO.getCodigo();
                tipoServico = TipoServicoEnum.AUTORIZACAO.getTipoRetorno();

                if (codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(Integer.valueOf(cStat)).equals(CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())) {
                    situacaoDocumento = SituacaoDocumentoEnum.AUTORIZADO.getCodigo();
                } else {
                    situacaoDocumento = SituacaoDocumentoEnum.CANCELADO.getCodigo();
                }
            } else if (docInfo.getResNFe() != null) {
                ResNFe resNFe = docInfo.getResNFe();

                chaveAcesso = resNFe.getChNFe();
                idEmissor = resNFe.getCNPJ() == null ? Long.parseLong(resNFe.getCPF()) : Long.parseLong(resNFe.getCNPJ());
                dataHoraEmissao = DateUtils.iso8601ToCalendar(resNFe.getDhRecbto()).getTime();
                versao = resNFe.getVersao();
                pontoDocumento = PontoDocumentoEnum.PROCESSADO.getCodigo();
                pontoEvento = PontoDocumentoEnum.RESUMO_NFE.getCodigo();

                if ("1".equals(resNFe.getCSitNFe())) {
                    situacaoDocumento = SituacaoDocumentoEnum.AUTORIZADO.getCodigo();
                } else if ("2".equals(resNFe.getCSitNFe())) {
                    situacaoDocumento = SituacaoDocumentoEnum.DENEGADO.getCodigo();
                } else {
                    situacaoDocumento = SituacaoDocumentoEnum.CANCELADO.getCodigo();
                }
            }

            DocumentoFiscal docu = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);

            Long idDocumentoFiscal;
            if (docu == null) {
                DocumentoFiscal documentoFiscal = new DocumentoFiscal();

                ChaveAcessoNFe chaveAcessoNFe = ChaveAcessoNFe.unparseKey(chaveAcesso);

                documentoFiscal.setTipoDocumentoFiscal(TIPO_DOCUMENTO_FISCAL_NFE);
                documentoFiscal.setIdEmissor(idEmissor);
                documentoFiscal.setNumeroDocumentoFiscal(Long.parseLong(chaveAcessoNFe.nNF));
                documentoFiscal.setSerieDocumentoFiscal(Long.parseLong(chaveAcessoNFe.serie));
                documentoFiscal.setDataHoraEmissao(dataHoraEmissao);
                Integer codigoIbge = Integer.valueOf(chaveAcessoNFe.cUF);
                documentoFiscal.setIdEstado(estadoRepository.findByCodigoIbge(codigoIbge).getId());
                documentoFiscal.setVersao(versao);
                documentoFiscal.setChaveAcesso(chaveAcesso);
                documentoFiscal.setChaveAcessoEnviada(chaveAcesso);
                documentoFiscal.setIdPonto(pontoDocumento);
                documentoFiscal.setSituacaoAutorizador(cStat);
                documentoFiscal.setSituacaoDocumento(situacaoDocumento);
                documentoFiscal.setSituacao(SituacaoEnum.LIQUIDADO.getCodigo());
                documentoFiscal.setAnoDocumentoFiscal(FazemuUtils.getFullYearFormat(Integer.valueOf(chaveAcessoNFe.dataAAMM.substring(0, 2))));
                documentoFiscal.setTipoEmissao(Long.parseLong(chaveAcessoNFe.tpEmis));
                documentoFiscal.setIdDestinatario(emissorRaizFilial.getIdFilial());
                documentoFiscal.setUsuarioReg(usuario);
                documentoFiscal.setUsuario(usuario);

                idDocumentoFiscal = documentoFiscalRepository.insert(documentoFiscal);

            } else {
                idDocumentoFiscal = docu.getId();
            }

            if (docu == null
                    || (docu != null
                    && "".equals(docu.getIdSistema()))) {
                Long idXmlClob = documentoClobRepository.insert(DocumentoClob.build(null, docInfo.getConteudoDecode(), usuario));
                documentoEventoRepository.insert(DocumentoEvento.build(null, idDocumentoFiscal, pontoEvento, tipoServico, null, idXmlClob, usuario));
            }

            emissorRaizFilialService.updateUltimoNSU(emissorRaizFilial.getIdFilial(), docInfo.getUltimoNSU(), usuario);
        }

    }

    private RetDistDFeInt enviaXml(NfeDadosMsg nfeDadosMsg) throws Exception {
        NfeDistDFeInteresseResponse response = distribuicaoDocumentosService.process(nfeDadosMsg, 0);
        NfeDistDFeInteresseResponse.NfeDistDFeInteresseResult result = response.getNfeDistDFeInteresseResult();

        List<Object> contentList = result.getContent();
        Object content = contentList.get(0);
        return (RetDistDFeInt) contextDistribuicao.createUnmarshaller().unmarshal((Node) content);
    }

    private NfeDadosMsg montaXMLDistribuicaoDFeDistNSU(EmissorRaizFilial emissorRaizFilial) {
        try {
            DistDFeInt.DistNSU nsu = new DistNSU();
            nsu.setUltNSU(emissorRaizFilial.getUltimoNSU());

            DistDFeInt distDFeInt = new DistDFeInt();
            distDFeInt.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
            distDFeInt.setCUFAutor("35"); // FIXME: Verificar

            distDFeInt.setCNPJ(StringUtils.leftPad(String.valueOf(emissorRaizFilial.getIdFilial()), 14, STR_ZERO));
            distDFeInt.setDistNSU(nsu);
            distDFeInt.setVersao(ServicosEnum.DISTRIBUICAO_DFE.getVersao());

            Document document = XMLUtils.createNewDocument();
            contextDistribuicao.createMarshaller().marshal(distDFeInt, document);

            NfeDadosMsg nfeDadosMsg = new NfeDadosMsg();
            nfeDadosMsg.getContent().add(document.getDocumentElement());

            return nfeDadosMsg;

        } catch (Exception e) {
            LOGGER.error("distribuirDocumentos (scheduled) - montaXmlDistribuicaoDFe {} ", ExceptionUtils.getStackTrace(e), e);
            return null;
        }
    }

    private List<EmissorRaizFilial> listaFiliais() throws Exception {
        Map<String, String> filtros = new HashMap<>();
        filtros.put("inConsultaDocumento", "S");
        return emissorRaizFilialService.listByFiltros(filtros);
    }

}
