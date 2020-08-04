package com.b2wdigital.fazemu.scheduled;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.EstadoConfiguracaoRepository;
import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.repository.LoteEventoRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.EstadoTipoEmissaoService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.business.service.TipoEmissaoService;
import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.domain.EstadoConfiguracao;
import com.b2wdigital.fazemu.domain.EstadoTipoEmissao;
import com.b2wdigital.fazemu.enumeration.TipoEmissaoEnum;
import com.b2wdigital.fazemu.exception.FazemuScheduledException;
import com.b2wdigital.fazemu.integration.dao.redis.RedisOperationsDao;
import com.b2wdigital.fazemu.service.impl.TelegramService;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.LoteUtils;
import com.google.common.collect.Maps;
import java.net.UnknownHostException;

/**
 * Entrar EPEC Automatico Scheduled.
 *
 * @author Marcelo Oliveira {marcelo.doliveira@b2wdigital.com}
 * @version 1.0
 */
@Service
public class EntrarEPECAutomaticoScheduled {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntrarEPECAutomaticoScheduled.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    private static final String KEY = RedisOperationsDao.KEY_SEMAFORO_ENTRAR_EPEC_AUTOMATICO;
    private static final String LOCK_HOST_EM_PROCESSAMENTO = "LOCK_ENTRAR_EPEC_AUTOMATICO";
    private static final String LOCK_EM_SELECAO_DE_DOCUMENTOS = "LOCK_ENTRAR_EPEC_AUTOMATICO_EM_SELECAO_DE_DOCUMENTOS";

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private EstadoConfiguracaoRepository estadoConfiguracaoRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private LoteEventoRepository loteEventoRepository;

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    protected TipoEmissaoService tipoEmissaoService;

    @Autowired
    private EstadoTipoEmissaoService estadoTipoEmissaoService;

    @Autowired
    private TelegramService telegramService;

    @Scheduled(fixedDelay = 40_000L) //de 4 em 4 minutos
    public void entrarEPECAutomatico() throws UnknownHostException {
        LOGGER.info("entrarEPECAutomatico (scheduled) - INICIO");

        String statusScheduled = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_SCHEDULED_ENTRAR_EPEC_AUTOMATICO);
        if (!"ON".equals(statusScheduled)) {
            throw new FazemuScheduledException("Chave de robo desligada");
        }

        String myHostLock = LoteUtils.getMyHost() + "_" + LOCK_HOST_EM_PROCESSAMENTO;

        if (cacheLoteRepository.setIfAbsent(myHostLock, true, 3, TimeUnit.MINUTES)) {

            Map<Estado, Long> mapEstadosEleitos = getEstadosElegiveis();

            if (mapEstadosEleitos != null) {
                LOGGER.info("entrarEPECAutomatico (scheduled) - mapEstadosEleitos {}", mapEstadosEleitos);

                String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);
                String justificativa = "ENTRADA AUTOMATICA EPEC";
                Date dataInicio = Calendar.getInstance().getTime();

                mapEstadosEleitos.entrySet().forEach((entry) -> {
                    try {
                        Estado estado = entry.getKey();
                        Long periodoEpec = entry.getValue();

                        LOGGER.debug("entrarEPECAutomatico {}", estado.getId());

                        // Valida tipo emissao atual
                        Integer tipoEmissaoAtual = tipoEmissaoService.getTipoEmissaoByCodigoIBGE(estado.getCodigoIbge().toString());

                        // Insere na estadoTipoEmissao quando tipoEmissaoAtual for Normal
                        if (TipoEmissaoEnum.NORMAL.getCodigo().equals(tipoEmissaoAtual)) {

                            // Periodo EPEC minutos
                            Calendar cal = Calendar.getInstance();
                            cal.add(Calendar.MINUTE, periodoEpec.intValue());

                            EstadoTipoEmissao estadoTipoEmissao = EstadoTipoEmissao.build(estado.getId(), dataInicio, cal.getTime(), TipoEmissaoEnum.EPEC.getCodigo().longValue(), justificativa, usuario);

                            estadoTipoEmissaoService.insert(estadoTipoEmissao);

                            String msg = telegramService.composeContingenciaMessage(TipoEmissaoEnum.EPEC.getDescricao().toUpperCase(), estado.getNome().toUpperCase());
                            telegramService.sendMessage(msg);
                        }

                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                });

            }

            // Excluir os eleitos do semaforo
            removerMembrosLista(mapEstadosEleitos);

            redisOperationsService.expiresKey(myHostLock, 1L, TimeUnit.MILLISECONDS);
        }
        
        LOGGER.info("entrarEPECAutomatico (scheduled) - FIM");
    }

    private Map<Estado, Long> getEstadosElegiveis() {

        Map<Estado, Long> mapEstados = Maps.newHashMap();

        if (cacheLoteRepository.setIfAbsent(LOCK_EM_SELECAO_DE_DOCUMENTOS, true, 1, TimeUnit.MINUTES)) {

            try {
                // Verifica todos os estados ativos
                List<Estado> listaEstadoAtivo = estadoRepository.listByAtivo();

                // Limita o processo a quantidade pre estabelecida
                int count = 0;
                Integer docsPorProcesso = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_ESTADOS_PROCESSADOS_COM_ERROS_LOTE, 5);

                if (CollectionUtils.isNotEmpty(listaEstadoAtivo)) {

                    for (Estado estado : listaEstadoAtivo) {

                        // Carrega informações para validar EPEC automatico
                        EstadoConfiguracao estadoConfiguracao = estadoConfiguracaoRepository.findByTipoDocumentoFiscalAndIdEstado(TIPO_DOCUMENTO_FISCAL_NFE, estado.getId());

                        if ("S".equals(estadoConfiguracao.getInEPECAutomatico())) {
                            // Periodo minutos atras
                            Calendar cal = Calendar.getInstance();
                            cal.add(Calendar.MINUTE, -estadoConfiguracao.getPeriodo().intValue());

                            // Valida a quantiadde de erros do estado para o periodo
                            Long countErros = loteEventoRepository.countByEstadoAndPeriodo(estado.getId(), cal.getTime());
                            LOGGER.info("entrarEPECAutomatico() - countErros {}", countErros);

                            // Caso a quantidade de erros de lote for maior que a parametrizacao, adiciona como elegivel
                            if (countErros > estadoConfiguracao.getQuantidadeMinimaRegistros()) {
                                mapEstados.put(estado, estadoConfiguracao.getPeriodoEPEC());
                                redisOperationsService.addToSet(KEY, estado.getId());
                                count++;
                                if (count == docsPorProcesso) {
                                    break;
                                }
                            }
                        }
                    }

                    if (mapEstados != null) {
                        redisOperationsService.expiresKey(KEY, 5L, TimeUnit.MINUTES);
                    }
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

        }

        redisOperationsService.expiresKey(LOCK_EM_SELECAO_DE_DOCUMENTOS, 1L, TimeUnit.MILLISECONDS);
        return mapEstados;
    }

    private void removerMembrosLista(Map<Estado, Long> lista) {
        LOGGER.info("entrarEPECAutomatico (scheduled) - removerMembrosLista {}", lista.size());

        lista.entrySet().forEach((entry) -> {
            redisOperationsService.removeFromSet(KEY, entry.getKey().getId());
        });
    }

}
