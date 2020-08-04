package com.b2wdigital.fazemu.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoLoteRepository;
import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.repository.LoteEventoRepository;
import com.b2wdigital.fazemu.business.repository.LoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.EmitirLoteService;
import com.b2wdigital.fazemu.business.service.FecharEnviarLoteService;
import com.b2wdigital.fazemu.domain.DocumentoLote;
import com.b2wdigital.fazemu.domain.Lote;
import com.b2wdigital.fazemu.domain.ResumoDocumentoFiscal;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.enumeration.PontoLoteEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;
import com.b2wdigital.fazemu.exception.FazemuDAOException;
import com.b2wdigital.fazemu.utils.FazemuUtils;

/**
 *
 * @author dailton.almeida
 */
@Service
public class EmitirLoteServiceImpl implements EmitirLoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmitirLoteServiceImpl.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private LoteEventoRepository loteEventoRepository;

    @Autowired
    private DocumentoLoteRepository documentoLoteRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private FecharEnviarLoteService fecharEnviarLoteService;

    @Autowired
    private EstadoRepository estadoRepository;

    @Override
    public ResumoLote emitirLote(ResumoDocumentoFiscal doc, ServicosEnum servico, boolean isReprocessamento) {
        try {
            LOGGER.info("emitirLote {}", doc.getIdDocFiscal());
            int xmlLen = doc.getTamanhoXML();

            ResumoLote lote = ResumoLote.build(0L);
            lote.setTipoDocumentoFiscal(doc.getTipoDocumentoFiscal());
            lote.setIdEmissor(doc.getIdEmissor());
            lote.setUf(doc.getUf());
            lote.setTipoEmissao(doc.getTipoEmissao());
            lote.setVersao(ServicosEnum.RECEPCAO_EVENTO_EPEC.equals(servico) ? ServicosEnum.RECEPCAO_EVENTO_EPEC.getVersao() : doc.getVersao());
            lote.setQuantidade(1);
            lote.setTamanho(xmlLen);
            lote.setDataAbertura(new Date());
            lote.setDataUltimaAlteracao(new Date());
            lote.setServico(servico.name());

            LOGGER.info("ResumoLote montado");

            String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

            int qtdMaximaDocLote;
            if (ServicosEnum.AUTORIZACAO_NFE.equals(servico)) {
                qtdMaximaDocLote = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_QTD_MAX_DOC_LOTE, 50);

            } else if (ServicosEnum.RECEPCAO_EVENTO_EPEC.equals(servico)) {
                qtdMaximaDocLote = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_QTD_MAX_DOC_LOTE_EPEC, 20);

            } else if (ServicosEnum.RECEPCAO_EVENTO_CANCELAMENTO.equals(servico)) {
                qtdMaximaDocLote = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_QTD_MAX_DOC_LOTE_CANCELAMENTO, 20);

            } else if (ServicosEnum.INUTILIZACAO.equals(servico)) {
                qtdMaximaDocLote = 1; // TODO parametrosInfra

            } else {
                qtdMaximaDocLote = 20; // TODO parametrosInfra
            }

            int tamanhoMaximoLote = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TAMANHO_MAXIMO_LOTE, 300_000); // default 300KB

            // verifica se existe lote aberto com as mesmas caracteristicas acima no cache
            ResumoLote loteAberto = isReprocessamento ? null : this.obterLoteAbertoSemelhante(lote, qtdMaximaDocLote, tamanhoMaximoLote);

            LOGGER.info("loteAberto : {}", loteAberto);

            if (loteAberto == null) {
                return novoLote(doc, isReprocessamento, lote, usuario);
            } else {

                LOGGER.info("emitirLote - usar lote existente {}", lote);

                loteAberto = applyLoteRulesCache(doc.getIdDocFiscal(), xmlLen, loteAberto.getIdLote(), lote,
                        qtdMaximaDocLote, tamanhoMaximoLote);

                if (loteAberto == null) {
                    return novoLote(doc, isReprocessamento, lote, usuario);
                }

                LOGGER.info("Aplicando regras de cache");

                Long idLote = loteAberto.getIdLote();

                LOGGER.info("Salvando eventos");

                documentoLoteRepository.insert(DocumentoLote.build(doc.getIdDocFiscal(), doc.getIdXML(), idLote, usuario, new Date()));
                loteEventoRepository.insert(idLote, PontoLoteEnum.ADICAO.getCodigo(), usuario, null, null);

                LOGGER.info("emitirLote - adicionado ao lote aberto {} o idDocFiscal {} ", loteAberto, doc.getIdDocFiscal());
                return loteAberto;
            }

        } catch (FazemuDAOException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }

    }

    private ResumoLote novoLote(ResumoDocumentoFiscal doc, boolean isReprocessamento, ResumoLote resumoLote, String usuario) {
        LOGGER.info("novoLote {}", resumoLote);

        // emite id do lote a partir do sequence
        Long idLote = loteRepository.emitirIdLote();
        resumoLote.setIdLote(idLote);
        resumoLote.setIdDocFiscalList(Arrays.asList(doc.getIdDocFiscal()));

        LOGGER.info("loteRepository.criarLote");
        Lote lote = resumoLote.toLote(usuario);
        lote.setIdEstado(estadoRepository.findByCodigoIbge(resumoLote.getUf()).getId());
        lote.setIdPonto(PontoLoteEnum.ABERTO.getCodigo());
        loteRepository.criarLote(lote);

        LOGGER.info("documentoLoteRepository.insert");
        documentoLoteRepository.insert(DocumentoLote.build(doc.getIdDocFiscal(), doc.getIdXML(), idLote, usuario, new Date()));

        LOGGER.info("loteEventoRepository.insert");
        loteEventoRepository.insert(idLote, PontoLoteEnum.ABERTO.getCodigo(), usuario, null, null);

        LOGGER.info("cacheLoteRepository.abrirLote");
        cacheLoteRepository.abrirLote(resumoLote);

        if (isReprocessamento) {
            LOGGER.info("Reprocessamento -- fechar lote {} imediatamente!!!", idLote);
            fecharEnviarLoteService.fecharEnviarLote(idLote);

        } else {
            long tempoEspera = 1000 * parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_ESPERA_FECHAMENTO_LOTE, 15); // default
            // 15seg
            LOGGER.info("TIMER TASK DE FECHAMENTO NO LOTE {} TEMPO DE ESPERA {}", resumoLote, tempoEspera);
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    LOGGER.info("TAREFA DE FECHAMENTO VAI SER EXECUTADA NO LOTE {}", resumoLote);
                    fecharEnviarLoteService.fecharEnviarLote(idLote);
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, tempoEspera);
        }

        LOGGER.info("emitirLote - emitir novo lote {}", resumoLote);
        return resumoLote;
    }

    private synchronized ResumoLote applyLoteRulesCache(long idDocFiscal, int xmlLen, long idLote,
            final ResumoLote lote, int qtdMaximaDocLote, int tamanhoMaximoLote) {

        ResumoLote loteAberto = this.obterLoteAbertoSemelhante(lote, qtdMaximaDocLote, tamanhoMaximoLote);

        if (loteAberto == null) {
            return null;
        }

        loteAberto.getIdDocFiscalList().add(idDocFiscal);
        loteAberto.setQuantidade(loteAberto.getQuantidade() + 1);
        loteAberto.setTamanho(loteAberto.getTamanho() + xmlLen);
        cacheLoteRepository.adicionarAoLote(loteAberto);
        return loteAberto;
    }

    protected ResumoLote obterLoteAbertoSemelhante(ResumoLote lote, int qtdMaximaDocLote, int tamanhoMaximoLote) {
        Set<Object> lotesAbertos = cacheLoteRepository.obterLotesAbertos();
        if (lotesAbertos != null) {
            for (Object chavesDosLotes : lotesAbertos) {
                LOGGER.debug("Objeto do lote aberto {} classe {}", chavesDosLotes, chavesDosLotes.getClass());
                ResumoLote loteAberto = cacheLoteRepository.consultarLote(Long.parseLong(chavesDosLotes.toString()));
                if (loteAberto != null) {
                    LOGGER.debug("LOTE ABERTO CONSULTADO {}", loteAberto);
                    if (FazemuUtils.obterRaizCNPJ(lote.getIdEmissor()) == FazemuUtils
                            .obterRaizCNPJ(loteAberto.getIdEmissor()) && lote.getUf().equals(loteAberto.getUf())
                            && lote.getTipoEmissao().equals(loteAberto.getTipoEmissao())
                            && lote.getVersao().equals(loteAberto.getVersao())
                            && lote.getQuantidade() + loteAberto.getQuantidade() < qtdMaximaDocLote
                            && lote.getTamanho() + loteAberto.getTamanho() < tamanhoMaximoLote
                            && lote.getServico().equals(loteAberto.getServico())) {
                        return loteAberto;
                    }
                }
            }
        }
        return null;
    }

    protected int getAsInteger(Map<String, String> map, String key, int defaultValue) {
        return map.containsKey(key) ? Integer.parseInt(map.get(key)) : defaultValue;
    }

    @Override
    public void reconstruirLotesAbertos() {
        LOGGER.info("reconstruirLotesAbertos");
        List<Lote> loteList = loteRepository.obterLotesPorSituacao(SituacaoLoteEnum.ABERTO.getCodigo());
        if (CollectionUtils.isNotEmpty(loteList)) {
            long currentTimeMillis = System.currentTimeMillis();
            String key = "lotesAbertos" + currentTimeMillis;
            loteList.stream().map((lote) -> {
                LOGGER.debug("reconstruirLotesAbertos lote {}", lote.getId());
                return lote;
            }).map((lote) -> {
                ResumoLote resumoLote = ResumoLote.fromLote(lote, documentoLoteRepository.listByIdDocFiscal(lote.getId()));
                resumoLote.setUf(estadoRepository.findById(lote.getIdEstado()).getCodigoIbge());
                return resumoLote;
            }).map((resumoLote) -> {
                resumoLote.setDataAbertura(new Date());
                return resumoLote;
            }).forEachOrdered((resumoLote) -> {
                cacheLoteRepository.abrirLote(resumoLote, key);
            });
            cacheLoteRepository.sobreporLotesAbertos(key);
        }
    }

}
