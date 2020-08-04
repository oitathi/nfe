package com.b2wdigital.fazemu.presentation.web.service.rest;

import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.service.KafkaProducerService;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.utils.FazemuUtils;

@RestController
public class CallbackRestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackRestService.class);
    private static final String XML_HEADER = "<?xml version='1.0' encoding='UTF-8'?>".replace('\'', '"'); //usa replace para nao ter que escapar tantas aspas

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @GetMapping(value = "/rest/invokeCallback", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String invokeCallback(@RequestParam(required = true) String chaveAcesso, @RequestParam(required = true) String tipoServico) {
        LOGGER.debug("KafkaRestService: invokeCallback {} ", chaveAcesso);

        chaveAcesso = FazemuUtils.normalizarChaveAcesso(chaveAcesso);
        DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);

        if (documentoFiscal == null) {
            return "Documento Fiscal " + chaveAcesso + " n\u00E3o encontrado.";
        } else {
            String xmlProcessado = documentoClobRepository.getXmlRetornoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.getByTipoRetorno(tipoServico));
            xmlProcessado = xmlProcessado.startsWith(XML_HEADER) ? xmlProcessado.replace(XML_HEADER, StringUtils.EMPTY).trim() : xmlProcessado.trim();

            TipoServicoEnum tipoServicoEnum = TipoServicoEnum.getByTipoRetorno(tipoServico);
            if (tipoServicoEnum == null) {
                return "Tipo de Servico " + tipoServico + " n\u00E3o encontrado.";
            }

            try {
                kafkaProducerService.invokeCallback(documentoFiscal, xmlProcessado, tipoServicoEnum);
                return "Sucesso " + chaveAcesso;
            } catch (Exception e) {
                LOGGER.error("Erro ao postar no controller " + e.getMessage(), e);
                return "Erro " + e;
            }
        }
    }

    @PostMapping(value = "/rest/invokeCallbackWithXML", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String invokeCallbackWithXML(@RequestBody(required = true) String requestPayload, @RequestParam(required = true) String chaveAcesso, @RequestParam(required = true) String tipoServico) {
        LOGGER.debug("KafkaRestService: invokeCallbackWithXML {} chaveAcesso {} tipoServico {}", requestPayload, chaveAcesso, tipoServico);

        chaveAcesso = FazemuUtils.normalizarChaveAcesso(chaveAcesso);
        DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);

        if (documentoFiscal == null) {
            return "Documento Fiscal " + chaveAcesso + " n\u00E3o encontrado.";
        } else {

            TipoServicoEnum tipoServicoEnum = TipoServicoEnum.getByTipoRetorno(tipoServico);
            if (tipoServicoEnum == null) {
                return "Tipo de Servico " + tipoServico + " n\u00E3o encontrado.";
            }

            try {
                kafkaProducerService.invokeCallback(documentoFiscal, requestPayload, tipoServicoEnum);
                return "Sucesso " + chaveAcesso;
            } catch (Exception e) {
                LOGGER.error("Erro ao postar no controller " + e.getMessage(), e);
                return "Erro " + e;
            }
        }
    }

}
