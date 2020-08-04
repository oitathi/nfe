package com.b2wdigital.fazemu.presentation.web.service.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.EmissorRaizCertificadoDigitalRepository;
import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.repository.LoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.repository.RedisOperationsRepository;
import com.b2wdigital.fazemu.business.repository.TipoEmissaoRepository;
import com.b2wdigital.fazemu.business.service.ConsultarReciboService;
import com.b2wdigital.fazemu.business.service.ConsultarStatusServicoService;
import com.b2wdigital.fazemu.business.service.EmitirLoteService;
import com.b2wdigital.fazemu.business.service.FecharEnviarLoteService;
import com.b2wdigital.fazemu.business.service.LoteOperationsService;
import com.b2wdigital.fazemu.domain.CodigoRetornoAutorizador;
import com.b2wdigital.fazemu.domain.EmissorRaizCertificadoDigital;
import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.domain.Lote;
import com.b2wdigital.fazemu.domain.ResumoDocumentoFiscal;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.domain.TipoEmissao;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceUrlNotFoundException;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 *
 * @author dailton.almeida
 */
@RestController
public class RedisTesterRedisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTesterRedisService.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private RedisOperations<String, Object> redisOperations;

    @Autowired
    private RedisOperations<String, String> stringRedisOperations;

    @Autowired
    private CacheLoteRepository cacheLoteRepository; //no rest service de verdade nao injetar o repositorio, mas o business service

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private EmitirLoteService emitirLoteService;

    @Autowired
    private FecharEnviarLoteService fecharEnviarLoteService;

    @Autowired
    private ConsultarReciboService consultarReciboService;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private EmissorRaizCertificadoDigitalRepository emissorRaizCertificadoDigitalRepository;

    @Autowired
    private TipoEmissaoRepository tipoEmissaoRepository;

    @Autowired
    private ConsultarStatusServicoService consultarStatusServicoService;

    @Autowired
    private RedisOperationsRepository redisOperationsRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private LoteOperationsService loteOperationsService;

    private static final String DASHBOARD_CACHE_KEY = "DASHBOARD_CACHE_KEY";

    @PostMapping("/redistester/abrirlote")
    public void abrirLote(@RequestParam("idLote") Long idLote, @RequestParam("idDocFiscal") Long idDocFiscal) {
        LOGGER.info("abrirLote idLote {} idDocFiscal {}", idLote, idDocFiscal);
        ResumoLote lote = ResumoLote.build(idLote);
        lote.setIdDocFiscalList(Arrays.asList(idDocFiscal));
        cacheLoteRepository.abrirLote(lote);
    }

    @PostMapping("/redistester/adicionaraolote")
    public void adicionarAoLote(@RequestParam("idLote") Long idLote, @RequestParam("idDocFiscal") Long idDocFiscal) {
        LOGGER.info("adicionarAoLote idLote {} idDocFiscal {}", idLote, idDocFiscal);

        ResumoLote lote = cacheLoteRepository.consultarLote(idLote);
        lote.getIdDocFiscalList().add(idDocFiscal);
        cacheLoteRepository.adicionarAoLote(lote);
    }

    @PostMapping("/redistester/emitirlote")
    public void emitirLote(@RequestParam("idEmissor") Long idEmissor,
            @RequestParam("tipoDocumentoFiscal") String tipoDocumentoFiscal,
            @RequestParam("uf") Integer uf,
            @RequestParam("municipio") Long municipio,
            @RequestParam("tipoEmissao") Integer tipoEmissao,
            @RequestParam("idDocFiscal") Long idDocFiscal,
            @RequestBody String xml
    ) {
        LOGGER.info("emitirLote idEmissor {} uf {} tipoEmissao {} idDocFiscal {} xml {}",
                idEmissor, uf, tipoEmissao, idDocFiscal, xml);

        ResumoDocumentoFiscal doc = ResumoDocumentoFiscal.build(idDocFiscal, tipoDocumentoFiscal, idEmissor, uf, municipio, tipoEmissao, "4.00", 0L, xml.length());
        LOGGER.info("emitirLote doc {}", doc);
        emitirLoteService.emitirLote(doc, ServicosEnum.AUTORIZACAO_NFE, false);
    }

    @PostMapping("/redistester/fecharlote")
    public void fecharLote(@RequestParam("idLote") Long idLote) {
        LOGGER.info("fecharLote idLote {}", idLote);
        cacheLoteRepository.fecharLote(idLote);
    }

    @GetMapping("/redistester/consultarcachettl")
    public Map<String, Long> consultarCacheTTL(@RequestParam(value = "pattern", required = false, defaultValue = "*") String pattern) {
        LOGGER.debug("consultarCacheTTL pattern {}", pattern);
        return SetUtils.emptyIfNull(redisOperations.keys(pattern))
                .stream()
                .collect(Collectors.toMap(Function.identity(), key -> redisOperations.getExpire(key)));
    }

    @GetMapping("/redistester/consultarcache")
    public Map<String, Object> consultarCache(@RequestParam(value = "pattern", required = false, defaultValue = "*") String pattern) {
        LOGGER.debug("consultarCache");
        Map<String, Object> result = Maps.newTreeMap();

        Set<String> redisKeys = redisOperations.keys(pattern);
        for (String key : redisKeys) {
            Object obj;
            if (DataType.STRING.equals(redisOperations.type(key))) {
                try {
                    obj = redisOperations.opsForValue().get(key);
                } catch (Exception e) {
                    obj = stringRedisOperations.opsForValue().get(key);
                }
                result.put(key, obj);
            } else if (DataType.SET.equals(redisOperations.type(key))) {
                result.put(key, redisOperations.opsForSet().members(key));
            }
        }

//        consultarCache(result, "lotesAbertos"    , cacheLoteRepository.obterLotesAbertos());
//        consultarCache(result, "lotesFechados"   , cacheLoteRepository.obterLotesFechados());
//        consultarCache(result, "lotesEnviados"   , cacheLoteRepository.obterLotesEnviados());
//        consultarCache(result, "lotesConsultando", cacheLoteRepository.obterLotesConsultando());
//        consultarCache(result, "lotesFinalizados", cacheLoteRepository.obterLotesFinalizados());
//        consultarCache(result, "lotesCancelados" , cacheLoteRepository.obterLotesCancelados());
        return result;
    }

    @GetMapping("/redistester/paib")
    public void getParametroBlob(HttpServletResponse response, @RequestParam("idParametro") String idParametro) {
        try {
            byte[] byteArray = parametrosInfraRepository.getAsByteArray(TIPO_DOCUMENTO_FISCAL_NFE, idParametro);
            IOUtils.write(byteArray, response.getOutputStream());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @GetMapping("/redistester/edig")
    public void getCertificadoDigital(HttpServletResponse response, @RequestParam("idEmissorRaiz") Long idEmissorRaiz) {
        try {
            EmissorRaizCertificadoDigital edig = emissorRaizCertificadoDigitalRepository.findByIdEmissorRaiz(idEmissorRaiz);
            IOUtils.write(edig.getCertificadoBytes(), response.getOutputStream());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @GetMapping("/redistester/reconstruircache")
    public void reconstruirCache() {
        try {
            emitirLoteService.reconstruirLotesAbertos();
            fecharEnviarLoteService.reconstruirLotesFechados();
            consultarReciboService.reconstruirLotesEnviados();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @GetMapping("/redistester/consultarlotes")
    public void consultarLotes() {
        try {
            List<Lote> loteList = loteRepository.obterLotesPorSituacao(SituacaoLoteEnum.ENVIADO.getCodigo());
            for (Lote lote : loteList) {
                loteOperationsService.reconstruirLote(lote.getId());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @GetMapping(value = "/redistester/statusservico", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String consultarStatusServico(//
            @RequestParam(value = "ufibge", required = false) Integer ufIBGE //
            ,
             @RequestParam(value = "tpEmissao", required = false) Integer tpEmissao //
            ,
             @RequestParam(value = "versao", required = false) String versao) throws Exception {

        if (ufIBGE == null) {

            String jsonResult = (String) redisOperationsRepository.getKeyValue(DASHBOARD_CACHE_KEY);

            if (StringUtils.isNotEmpty(jsonResult)) {
                return jsonResult;
            }

            ExecutorService executorService = Executors.newCachedThreadPool();

            List<Callable<Triple<String, Integer, CodigoRetornoAutorizador>>> callableList = Lists.newArrayList();

            for (Estado estado : estadoRepository.listByAtivo()) {
                for (TipoEmissao tipoEmissao : tipoEmissaoRepository.listAtivosByIdEstado(estado.getId())) {
                    callableList.add(() -> {
                        CodigoRetornoAutorizador crau;
                        Integer codigoIbge = estado.getCodigoIbge();
                        Integer idTpEmissao = tipoEmissao.getId().intValue();
                        try {
                            crau = consultarStatusServicoService.process(codigoIbge, idTpEmissao, versao);
                        } catch (FazemuServiceUrlNotFoundException e) {
                            crau = CodigoRetornoAutorizador.build(Integer.valueOf(FazemuUtils.ERROR_CODE));
                            crau.setSituacaoAutorizador("NA");
                            crau.setDescricao("N\u00E3o se aplica");
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage());
                            crau = CodigoRetornoAutorizador.build(Integer.valueOf(FazemuUtils.ERROR_CODE));
                            crau.setSituacaoAutorizador("-");
                            crau.setDescricao(e.getMessage());
                        }
                        return Triple.of(estado.getSigla(), idTpEmissao, crau);
                    });
                }
            }

            try {
                // invoke all callables
                List<Future<Triple<String, Integer, CodigoRetornoAutorizador>>> futureList = executorService
                        .invokeAll(callableList);

                Map<String, Map<Integer, CodigoRetornoAutorizador>> result = Maps.newHashMap();

                for (Future<Triple<String, Integer, CodigoRetornoAutorizador>> future : futureList) {
                    Triple<String, Integer, CodigoRetornoAutorizador> triple = future.get(); // blocks
                    if (result.containsKey(triple.getLeft())) {
                        result.get(triple.getLeft()).put(triple.getMiddle(), triple.getRight());
                    } else {
                        Map<Integer, CodigoRetornoAutorizador> map = Maps.newHashMap();
                        map.put(triple.getMiddle(), triple.getRight());
                        result.put(triple.getLeft(), map);
                    }
                }

                jsonResult = new ObjectMapper().writeValueAsString(result);

            } finally {
                executorService.shutdown();
            }

            cacheLoteRepository.setIfAbsent(DASHBOARD_CACHE_KEY, jsonResult, 5, TimeUnit.MINUTES);

            return jsonResult;
        }

        return null;
    }

    @PostMapping("/redistester/moverlote")
    public void moverLote(@RequestParam("idLote") Long idLote, @RequestParam("setOrigem") String setOrigem, @RequestParam("setDestino") String setDestino) {
        LOGGER.info("moverLote idLote {} setOrigem {} setDestino {}", idLote, setOrigem, setDestino);
        cacheLoteRepository.moverLote(idLote, setOrigem, setDestino);
    }

}
