package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2winc.corpserv.message.exception.InternalServerException;
import com.b2winc.corpserv.message.exception.UnauthorizedException;
import com.google.common.collect.Maps;
@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class MonitoriaLoteController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoriaLoteController.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Autowired
    private CacheLoteRepository cacheLoteRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @GetMapping(value = "/lote/status", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    Map<String, Map<Boolean, Integer>> qtdLotesPorSituacao(
            @RequestHeader(value = "X-B2W-UserId", required = false) String userId,
            @RequestHeader(value = "X-B2W-Token", required = false) String token,
            @RequestHeader(value = "X-B2W-System", required = false) String system,
            @RequestHeader(value = "Authorization", required = true) String authorization) throws UnauthorizedException, InternalServerException {
        try {
            LOGGER.debug("MonitoriaLoteController: qtdLotesPorSituacao");

            Map<String, Map<Boolean, Integer>> result = Maps.newTreeMap();
            int loteIntervaloMonitoracaoOK = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, "LOTE_INTERVALO_MONITORACAO_OK", 60);

            result.put(SituacaoLoteEnum.ABERTO.getDescricao(), partitionByDateRange(cacheLoteRepository.obterLotesAbertos(), loteIntervaloMonitoracaoOK));
            result.put(SituacaoLoteEnum.FECHADO.getDescricao(), partitionByDateRange(cacheLoteRepository.obterLotesFechados(), loteIntervaloMonitoracaoOK));
            result.put(SituacaoLoteEnum.ENVIADO.getDescricao(), partitionByDateRange(cacheLoteRepository.obterLotesEnviados(), loteIntervaloMonitoracaoOK));
            result.put(SituacaoLoteEnum.LIQUIDADO.getDescricao(), partitionByDateRange(cacheLoteRepository.obterLotesFinalizados(), loteIntervaloMonitoracaoOK));
            result.put(SituacaoLoteEnum.CANCELADO.getDescricao(), partitionByDateRange(cacheLoteRepository.obterLotesCancelados(), loteIntervaloMonitoracaoOK));
            result.put(SituacaoLoteEnum.ERRO.getDescricao(), partitionByDateRange(cacheLoteRepository.obterLotesErro(), loteIntervaloMonitoracaoOK));

            return result;

        } catch (Exception e) {
            throw new InternalServerException();
        }
    }

    private Map<Boolean, Integer> partitionByDateRange(Set<Object> loteSet, int seconds) {
        Map<Boolean, Integer> result = Maps.newHashMapWithExpectedSize(2);
        result.put(Boolean.FALSE, 0);
        result.put(Boolean.TRUE, 0);
        SetUtils.emptyIfNull(loteSet)
                .stream()
                .map(obj -> ((Integer) obj).longValue()) //obj == idLote em tipo Integer
                .map(idLote -> cacheLoteRepository.consultarLote(idLote))
                .map(lote -> checkRange(lote, seconds))
                .forEach(b -> {
                    result.put(b, 1 + result.get(b));
                });
        return result;
    }

    private boolean checkRange(ResumoLote lote, int seconds) {
        Calendar cal = Calendar.getInstance();
        Date d2 = cal.getTime();
        cal.add(Calendar.SECOND, -seconds);
        Date d1 = cal.getTime();
        return checkRange(d1, lote, d2);
    }

    private boolean checkRange(Date d1, ResumoLote lote, Date d2) {
        return lote != null && lote.getDataAbertura() != null && Range.between(d1, d2).contains(lote.getDataAbertura());
    }

}
