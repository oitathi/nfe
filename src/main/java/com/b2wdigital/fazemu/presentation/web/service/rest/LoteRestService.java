package com.b2wdigital.fazemu.presentation.web.service.rest;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.LoteOperationsService;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2winc.corpserv.message.exception.InternalServerException;
import com.b2winc.corpserv.message.exception.NotFoundException;
import com.b2winc.corpserv.message.exception.UnauthorizedException;
import com.google.common.collect.Maps;

/**
 *
 * @author dailton.almeida
 */
@RestController
public class LoteRestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoteRestService.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();
    private static final String DEFAULT_MAPPING = "/lote";

    @Autowired
    private CacheLoteRepository cacheLoteRepository;
    @Autowired
    private EstadoRepository estadoRepository;
    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;
    @Autowired
    private LoteOperationsService loteOperationsService;

    @GetMapping(value = DEFAULT_MAPPING + "/{idLote}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResumoLote consultarLote(@PathVariable("idLote") Long idLote) throws NotFoundException {
        LOGGER.debug("consultarLote idLote {}", idLote);
        ResumoLote rs = cacheLoteRepository.consultarLote(idLote);
        if (rs == null) {
            throw new NotFoundException("ResumoLote não encontrado " + idLote);
        }
        return rs;
    }

    @GetMapping(value = DEFAULT_MAPPING, params = "situacao", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<ResumoLote> consultarLotesPorSituacao(@RequestParam("situacao") String situacao) {
        LOGGER.debug("consultarLotesPorSituacao situacao {}", situacao);
        Set<Object> loteSet = null;
        switch (SituacaoLoteEnum.getByCodigo(situacao)) {
            case ABERTO:
                loteSet = cacheLoteRepository.obterLotesAbertos();
                break;
            case FECHADO:
                loteSet = cacheLoteRepository.obterLotesFechados();
                break;
            case ENVIADO:
                loteSet = cacheLoteRepository.obterLotesEnviados();
                break;
            case LIQUIDADO:
                loteSet = cacheLoteRepository.obterLotesFinalizados();
                break;
            case CANCELADO:
                loteSet = cacheLoteRepository.obterLotesCancelados();
                break;
            case ERRO:
                break;
            default:
                break;
        }
        List<ResumoLote> result = SetUtils.emptyIfNull(loteSet)
                .stream()
                .mapToLong(obj -> ((Integer) obj).longValue())
                .mapToObj(cacheLoteRepository::consultarLote)
                .collect(Collectors.toList());
        LOGGER.debug("consultarLotesPorSituacao situacao {} result {}", situacao, result);
        return result;
    }

    protected boolean checkRange(Date d1, ResumoLote lote, Date d2) {
        return lote != null && lote.getDataAbertura() != null && Range.between(d1, d2).contains(lote.getDataAbertura());
    }

    protected boolean checkRange(ResumoLote lote, int seconds) {
        Calendar cal = Calendar.getInstance();
        Date d2 = cal.getTime();
        cal.add(Calendar.SECOND, -seconds);
        Date d1 = cal.getTime();
        return checkRange(d1, lote, d2);
    }

    protected Map<Boolean, Integer> partitionByDateRange(Set<Object> loteSet, int seconds) {
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
        LOGGER.debug("partitionByDateRange result {}", result);
        return result;
    }

    @RequestMapping(value = DEFAULT_MAPPING + "/ciclo", method = RequestMethod.GET)

    public @ResponseBody
    Map<String, Map<Boolean, Integer>> qtdLotesPorSituacao(
            @RequestHeader(value = "X-B2W-UserId", required = false) String userId,
            @RequestHeader(value = "X-B2W-Token", required = false) String token,
            @RequestHeader(value = "X-B2W-System", required = false) String system,
            @RequestHeader(value = "Authorization", required = false) String authorization) throws UnauthorizedException, InternalServerException {
        try {

            Map<String, Map<Boolean, Integer>> result = Maps.newTreeMap();
            int loteIntervaloMonitoracaoOK = parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, "LOTE_INTERVALO_MONITORACAO_OK", 60);

            result.put(SituacaoLoteEnum.ABERTO.getDescricao(), partitionByDateRange(cacheLoteRepository.obterLotesAbertos(), loteIntervaloMonitoracaoOK));
            result.put(SituacaoLoteEnum.FECHADO.getDescricao(), partitionByDateRange(cacheLoteRepository.obterLotesFechados(), loteIntervaloMonitoracaoOK));
            result.put(SituacaoLoteEnum.ENVIADO.getDescricao(), partitionByDateRange(cacheLoteRepository.obterLotesEnviados(), loteIntervaloMonitoracaoOK));
            result.put(SituacaoLoteEnum.LIQUIDADO.getDescricao(), partitionByDateRange(cacheLoteRepository.obterLotesFinalizados(), loteIntervaloMonitoracaoOK));
            result.put(SituacaoLoteEnum.CANCELADO.getDescricao(), partitionByDateRange(cacheLoteRepository.obterLotesCancelados(), loteIntervaloMonitoracaoOK));

            return result;

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new InternalServerException();
        }
    }

    @GetMapping(value = DEFAULT_MAPPING + "/ciclouf", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, Map<String, Integer>> lotesPorUFeSituacao() {
        try {
            LOGGER.debug("lotesPorUFeSituacao");
            Map<String, Map<String, Integer>> result = Maps.newTreeMap();

            this.lotesPorUFeSituacaoFixa(result, SituacaoLoteEnum.ABERTO, cacheLoteRepository.obterLotesAbertos());
            this.lotesPorUFeSituacaoFixa(result, SituacaoLoteEnum.FECHADO, cacheLoteRepository.obterLotesFechados());
            this.lotesPorUFeSituacaoFixa(result, SituacaoLoteEnum.ENVIADO, cacheLoteRepository.obterLotesEnviados());
            this.lotesPorUFeSituacaoFixa(result, SituacaoLoteEnum.LIQUIDADO, cacheLoteRepository.obterLotesFinalizados());
            this.lotesPorUFeSituacaoFixa(result, SituacaoLoteEnum.CANCELADO, cacheLoteRepository.obterLotesCancelados());

            LOGGER.debug("lotesPorUFeSituacao result {}", result);
            return result;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    protected void lotesPorUFeSituacaoFixa(Map<String, Map<String, Integer>> result, SituacaoLoteEnum situacaoLoteEnum, Set<Object> loteSet) {
        String chaveSituacao = situacaoLoteEnum.getDescricao();
        SetUtils.emptyIfNull(loteSet)
                .stream()
                .map(obj -> ((Integer) obj).longValue()) //obj == idLote em tipo Integer
                .map(idLote -> cacheLoteRepository.consultarLote(idLote))
                .filter(Objects::nonNull) //se lotes expirarem no futuro, podem estar nulos no cache
                .map(lote -> estadoRepository.findByCodigoIbge(lote.getUf()))
                .filter(Objects::nonNull)
                .forEach(estado -> {
                    if (result.containsKey(estado.getSigla())) {
                        Map<String, Integer> map = result.get(estado.getSigla());
                        map.put(chaveSituacao, map.containsKey(chaveSituacao) ? 1 + map.get(chaveSituacao) : 1);
                    } else {
                        Map<String, Integer> map = Maps.newTreeMap();
                        map.put(chaveSituacao, 1);
                        result.put(estado.getSigla(), map);
                    }
                });
    }

    @GetMapping(value = DEFAULT_MAPPING + "/cicloemissor", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<Long, Map<String, Integer>> lotesPorIdEmissorRaizSituacao() {
        try {
            LOGGER.debug("lotesPorIdEmissorRaizSituacao");
            Map<Long, Map<String, Integer>> result = Maps.newTreeMap();

            this.lotesPorIdEmissorRaizSituacaoFixa(result, SituacaoLoteEnum.ABERTO, cacheLoteRepository.obterLotesAbertos());
            this.lotesPorIdEmissorRaizSituacaoFixa(result, SituacaoLoteEnum.FECHADO, cacheLoteRepository.obterLotesFechados());
            this.lotesPorIdEmissorRaizSituacaoFixa(result, SituacaoLoteEnum.ENVIADO, cacheLoteRepository.obterLotesEnviados());
            this.lotesPorIdEmissorRaizSituacaoFixa(result, SituacaoLoteEnum.LIQUIDADO, cacheLoteRepository.obterLotesFinalizados());
            this.lotesPorIdEmissorRaizSituacaoFixa(result, SituacaoLoteEnum.CANCELADO, cacheLoteRepository.obterLotesCancelados());

            LOGGER.debug("lotesPorIdEmissorRaizSituacao result {}", result);
            return result;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    protected void lotesPorIdEmissorRaizSituacaoFixa(Map<Long, Map<String, Integer>> result, SituacaoLoteEnum situacaoLoteEnum, Set<Object> loteSet) {
        String chaveSituacao = situacaoLoteEnum.getDescricao();
        SetUtils.emptyIfNull(loteSet)
                .stream()
                .map(obj -> ((Integer) obj).longValue()) //obj == idLote em tipo Integer
                .map(idLote -> cacheLoteRepository.consultarLote(idLote))
                .filter(Objects::nonNull)
                .map(lote -> FazemuUtils.obterRaizCNPJ(lote.getIdEmissor()))
                .forEach((idEmissorRaiz) -> {
                    if (result.containsKey(idEmissorRaiz)) {
                        Map<String, Integer> map = result.get(idEmissorRaiz);
                        map.put(chaveSituacao, map.containsKey(chaveSituacao) ? 1 + map.get(chaveSituacao) : 1);
                    } else {
                        Map<String, Integer> map = Maps.newTreeMap();
                        map.put(chaveSituacao, 1);
                        result.put(idEmissorRaiz, map);
                    }
                });
    }

    @GetMapping(value = DEFAULT_MAPPING + "/reconstruirLote")
    public String reconstruirLote(@RequestParam("idLote") Long idLote) {
        try {
            LOGGER.debug("reconstruirLote {}", idLote);

            loteOperationsService.reconstruirLote(idLote);

            return "OK";
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

}
