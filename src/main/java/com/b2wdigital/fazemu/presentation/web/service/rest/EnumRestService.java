package com.b2wdigital.fazemu.presentation.web.service.rest;

import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author dailton.almeida
 */
@RestController
public class EnumRestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnumRestService.class);
    
    @GetMapping(value = "/enum/pontodocumento", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, String> pontosDocumento() {
        LOGGER.debug("pontosDocumento");
        return Arrays.stream(PontoDocumentoEnum.values())
                .collect(Collectors.toMap(PontoDocumentoEnum::getCodigo, PontoDocumentoEnum::getDescricao, (first, second) -> first));
    }

    @GetMapping(value = "/enum/situacaolote", params = {"type=mapa"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, String> situacoesLoteAsMap() {
        LOGGER.debug("situacoesLoteAsMap");
        return Arrays.stream(SituacaoLoteEnum.values())
                .collect(Collectors.toMap(SituacaoLoteEnum::getCodigo, SituacaoLoteEnum::getDescricao, (first, second) -> first));
    }
    
    @GetMapping(value = "/enum/situacaolote", params = {"type=codigos"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String[] situacoesLoteAsCodigos() {
        return SituacaoLoteEnum.codigos();
    }
    
    @GetMapping(value = "/enum/situacaolote", params = {"type=descricoes"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String[] situacoesLoteDescricoes() {
        return SituacaoLoteEnum.descricoes();
    }

    @GetMapping(value = "/enum/documentoretorno", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, String> documentoRetorno() {
        LOGGER.debug("documentoRetorno");
        return Arrays.stream(TipoServicoEnum.values())
                .collect(Collectors.toMap(TipoServicoEnum::getTipoRetorno, TipoServicoEnum::getDescricao, (first, second) -> first));
    }

}
