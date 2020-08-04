package com.b2wdigital.fazemu.scheduled;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.ConsultarProtocoloService;
import com.b2wdigital.fazemu.business.service.DocumentoClobService;
import com.b2wdigital.fazemu.business.service.DocumentoFiscalService;
import com.b2wdigital.fazemu.business.service.DocumentoRetornoService;
import com.b2wdigital.fazemu.business.service.LoteService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.DocumentoRetorno;
import com.b2wdigital.fazemu.domain.Lote;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.FazemuScheduledException;
import com.b2wdigital.fazemu.integration.dao.redis.RedisOperationsDao;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.LoteUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TretEvento;
import com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TretEvento.InfEvento;
import com.google.common.collect.Lists;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.w3c.dom.Document;

/**
 * Gerar Callback XML Scheduled.
 *
 * @author Marcelo Oliveira {marcelo.doliveira@b2wdigital.com}
 * @version 1.0
 */
@Service
public class GerarXMLFaltanteScheduled extends AbstractScheduled {

    private static final Logger LOGGER = LoggerFactory.getLogger(GerarXMLFaltanteScheduled.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    private static final String KEY = RedisOperationsDao.KEY_SEMAFORO_GERAR_XML_FALTANTE;
    private static final String LOCK_HOST_EM_PROCESSAMENTO = "LOCK_GERAR_XML_FALTANTE";
    private static final String LOCK_EM_SELECAO_DE_DOCUMENTOS = "LOCK_GERAR_XML_FALTANTE_EM_SELECAO_DE_DOCUMENTOS";

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    private DocumentoFiscalService documentoFiscalService;

    @Autowired
    private DocumentoRetornoService documentoRetornoService;

    @Autowired
    private DocumentoClobService documentoClobService;

    @Autowired
    private ConsultarProtocoloService consultarProtocoloService;

    @Autowired
    private LoteService loteService;

    @Autowired
    @Qualifier("nfeRecepcaoEventoManifestacaoContext")
    private JAXBContext contextManifestacao;

    @Scheduled(fixedDelay = 600_000L) //de 10 em 10 minutos
    public void gerarXMLFaltante() throws UnknownHostException {
        LOGGER.info("gerarXMLFaltante (scheduled) - INICIO");

        String statusScheduled = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_SCHEDULED_GERAR_XML_FALTANTE);
        if (!"ON".equals(statusScheduled)) {
            throw new FazemuScheduledException("Chave de robo desligada");
        }

        String myHostLock = LoteUtils.getMyHost() + "_" + LOCK_HOST_EM_PROCESSAMENTO;

        if (cacheLoteRepository.setIfAbsent(myHostLock, true, 3, TimeUnit.MINUTES)) {

            List<DocumentoRetorno> listaDocumentosEleitos = getDocumentosElegiveis();

            listaDocumentosEleitos.stream().forEach(documentoRetorno -> {

                try {
                    LOGGER.info("gerarXMLFaltante (scheduled) - tipoServico {}", documentoRetorno.getTipoServico());

                    DocumentoFiscal documentoFiscal = documentoFiscalService.findById(documentoRetorno.getIdDocumentoFiscal());

                    if (!TipoServicoEnum.MANIFESTACAO.getTipoRetorno().equals(documentoRetorno.getTipoServico())) {
                        consultarProtocoloService.atualizarConsultarProtocolo(documentoFiscal.getChaveAcessoEnviada(), documentoRetorno.getTipoServico(), false);
                    } else {
                        String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

                        TretEvento tRetEvento = montaManifestacao(documentoFiscal);

                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        factory.setNamespaceAware(true);
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document documentRetEvento = builder.newDocument();

                        contextManifestacao.createMarshaller().marshal(tRetEvento, documentRetEvento);

                        Long idClob = documentoClobService.insert(DocumentoClob.build(null, XMLUtils.convertDocumentToString(documentRetEvento), usuario));

                        Long tipoEvento = Long.valueOf(tRetEvento.getInfEvento().getTpEvento());
                        if (documentoRetornoService.findByIdDocFiscalAndTpServicoAndTpEvento(documentoRetorno.getIdDocumentoFiscal(), TipoServicoEnum.MANIFESTACAO.getTipoRetorno(), tipoEvento) == null) {
                            documentoRetornoService.insert(documentoRetorno.getIdDocumentoFiscal(), TipoServicoEnum.MANIFESTACAO.getTipoRetorno(), tipoEvento, idClob, usuario, usuario);
                        } else {
                            documentoRetornoService.update(documentoRetorno.getIdDocumentoFiscal(), TipoServicoEnum.MANIFESTACAO.getTipoRetorno(), tipoEvento, idClob, usuario);
                        }
                    }

                } catch (Exception e) {
                    LOGGER.error("gerarXMLFaltante (scheduled) - ERRO no documento fiscal {} {} ", documentoRetorno.getIdDocumentoFiscal(), e.getMessage(), e);
                }
            });

            // Excluir os eleitos do semaforo
            removerMembrosLista(listaDocumentosEleitos);

            redisOperationsService.expiresKey(myHostLock, 1L, TimeUnit.MILLISECONDS);
        }

        LOGGER.info("gerarXMLFaltante (scheduled) - FIM");
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
                cal.add(Calendar.MINUTE, -parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_MIN_EMITIR_DOCUMENTOS_PARADOS, 10));

                Calendar cal2 = Calendar.getInstance();
                cal2.add(Calendar.MINUTE, -parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_MAX_EMITIR_DOCUMENTOS_PARADOS, 300));

                List<DocumentoRetorno> listaDocumentoRetorno = documentoRetornoService.listByDateIntervalAndNotExistsDocl(TIPO_DOCUMENTO_FISCAL_NFE, cal.getTime(), cal2.getTime(), notIn);
                if (CollectionUtils.isNotEmpty(listaDocumentoRetorno)) {

                    // Limita o processo a quantidade pre estabelecida
                    int count = 0;
                    Integer docsPorProcesso = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_DOCUMENTOS_PROCESSADOS_EM_PARALELO, 30);

                    for (DocumentoRetorno documentoRetorno : listaDocumentoRetorno) {
                        //Verifica se o documento não esta ligado a um lote aberto/fechado/enviado
                        List<Lote> listaLotesAbertos = loteService.listByIdDocFiscalAndSituacao(documentoRetorno.getIdDocumentoFiscal(), SituacaoLoteEnum.ABERTO);
                        List<Lote> listaLotesFechados = loteService.listByIdDocFiscalAndSituacao(documentoRetorno.getIdDocumentoFiscal(), SituacaoLoteEnum.FECHADO);
                        List<Lote> listaLotesEnviados = loteService.listByIdDocFiscalAndSituacao(documentoRetorno.getIdDocumentoFiscal(), SituacaoLoteEnum.ENVIADO);

                        if (CollectionUtils.isEmpty(listaLotesAbertos) && CollectionUtils.isEmpty(listaLotesFechados) && CollectionUtils.isEmpty(listaLotesEnviados)) {
                            documentosElegiveis.add(documentoRetorno);

                            redisOperationsService.addToSet(KEY, documentoRetorno.getIdDocumentoFiscal());
                            count++;
                            if (count == docsPorProcesso) {
                                break;
                            }
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
        LOGGER.info("gerarXMLFaltante (scheduled) - removerMembrosLista {}", lista.size());

        lista.forEach((documento) -> {
            redisOperationsService.removeFromSet(KEY, documento.getIdDocumentoFiscal());
        });
    }

    private TretEvento montaManifestacao(DocumentoFiscal documentoFiscal) {
        LOGGER.info("gerarXMLFaltante (scheduled) -  montaManifestacao{}", documentoFiscal.getId());

        InfEvento infEvento = new InfEvento();
        infEvento.setTpAmb(parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE));
        infEvento.setVerAplic("AN_4.0.0");
        infEvento.setCOrgao("91");
        infEvento.setCStat("135");
        infEvento.setXMotivo("Evento registrado e vinculado a NF-e");
        infEvento.setChNFe(documentoFiscal.getChaveAcessoEnviada());

        TretEvento tRetEvento = new TretEvento();
        tRetEvento.setInfEvento(infEvento);

        return tRetEvento;
    }
}
