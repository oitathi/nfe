package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.repository.EstadoTipoEmissaoRepository;
import com.b2wdigital.fazemu.business.repository.RedisOperationsRepository;
import com.b2wdigital.fazemu.business.repository.TipoEmissaoRepository;
import com.b2wdigital.fazemu.business.service.ConsultarStatusServicoService;
import com.b2wdigital.fazemu.domain.CodigoRetornoAutorizador;
import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.domain.EstadoTipoEmissao;
import com.b2wdigital.fazemu.domain.TipoEmissao;
import com.b2wdigital.fazemu.enumeration.CodigoRetornoAutorizadorEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceUrlNotFoundException;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class FazemuWebController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FazemuWebController.class);
    private static final String DASHBOARD_CACHE_KEY = "DASHBOARD_CACHE_KEY";

    @Autowired
    private RedisOperations<String, Object> redisOperations;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private TipoEmissaoRepository tipoEmissaoRepository;

    @Autowired
    private EstadoTipoEmissaoRepository estadoTipoEmissaoRepository;

    @Autowired
    private ConsultarStatusServicoService consultarStatusServicoService;

    @Autowired
    private RedisOperationsRepository redisOperationsRepository;

    @Autowired
    @Qualifier("nfeRetAutorizacaoContext")
    private JAXBContext context;

    @GetMapping(value = "/servico/status")
    public String consultarStatusServico(//
            @RequestParam(value = "ufIbge", required = false) Integer codigoIbge //
            ,
             @RequestParam(value = "tipoEmissao", required = false) Integer tipoEmissao //
            ,
             @RequestParam(value = "versao", required = false) String versao) throws Exception {

        if (codigoIbge == null) {

            String jsonResult = (String) redisOperationsRepository.getKeyValue(DASHBOARD_CACHE_KEY);

            if (StringUtils.isNotEmpty(jsonResult)) {
                return jsonResult;
            }

            ExecutorService executorService = Executors.newCachedThreadPool();

            List<Callable<Triple<String, Integer, CodigoRetornoAutorizador>>> callableList = Lists.newArrayList();

            for (Estado estado : estadoRepository.listByAtivo()) {
                for (TipoEmissao tpEmissao : tipoEmissaoRepository.listAtivosByIdEstado(estado.getId())) {
                    callableList.add(() -> {
                        CodigoRetornoAutorizador crau;
                        Integer codIbge = estado.getCodigoIbge();
                        Integer idTipoEmissao = tpEmissao.getId().intValue();
                        try {
                            crau = consultarStatusServicoService.process(codIbge, idTipoEmissao, versao);
//                            String situacao = "A";
                            Estado esta = estadoRepository.findByCodigoIbge(codigoIbge);

                            if (CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(crau.getSituacaoAutorizador())) {
                                if (tipoEmissao != 1) {
                                    List<EstadoTipoEmissao> este = estadoTipoEmissaoRepository.listByEstadoAndTipoEmissao(esta.getId(),  tipoEmissao);
                                    if (este.isEmpty()) {
                                        crau.setSituacaoAutorizador("NU");
                                        crau.setDescricao("N\u00E3o se aplica");
                                    }
                                }
                            }
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
                        return Triple.of(estado.getSigla(), idTipoEmissao, crau);
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

            redisOperations.boundValueOps(DASHBOARD_CACHE_KEY).set(jsonResult, 5, TimeUnit.MINUTES);

            return jsonResult;
        } else if (codigoIbge != null
                && tipoEmissao != null) {
            CodigoRetornoAutorizador crau;
            
            Estado esta = estadoRepository.findByCodigoIbge(codigoIbge);
            try {
                crau = consultarStatusServicoService.process(codigoIbge, tipoEmissao, versao);
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

            if (CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(crau.getSituacaoAutorizador())) {
                if (tipoEmissao == 1) {
                    return crau.getSituacaoAutorizador();
                } else {
                    List<EstadoTipoEmissao> este = estadoTipoEmissaoRepository.listByEstadoAndTipoEmissao(esta.getId(), tipoEmissao);
                    if (este.isEmpty()) {
                        return "NU";
                    } else {
                        return crau.getSituacaoAutorizador();
                    }
                }
            } else {
                return crau.getSituacaoAutorizador();
            }
        }

        return null;
    }

    

}
