package com.b2wdigital.fazemu.presentation.web.service.rest;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import com.b2wdigital.fazemu.business.service.ImpressaoService;
import com.b2wdigital.fazemu.exception.NotFoundException;

@RestController
public class ImpressaoRestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImpressaoRestService.class);

    @Autowired
    private ImpressaoService impressaoService;

    @PostMapping(value = "/rest/impressaoPDF", produces = MediaType.TEXT_PLAIN_VALUE)
    public String impressaoPDF(HttpServletResponse response, @RequestBody(required = true) String base64, @RequestParam(required = true) String impressora) {
        try {
            LOGGER.debug("ImpressaoService: impressaoPDF");

            impressaoService.imprimirPDF(base64, impressora);

            int code = HttpStatus.OK.value();
            response.setStatus(code);
            return "<fazemu><code>" + code + "</code><message>SUCESSO</message></fazemu>";
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.BadRequest e) {
            int code = e.getRawStatusCode();
            response.setStatus(code);
            return "<error><errorCode>" + code + "</errorCode><message>" + e.getMessage() + "</message></error>";
        } catch (Exception e) {
            int code = HttpStatus.INTERNAL_SERVER_ERROR.value();
            response.setStatus(code);
            return "<error><errorCode>" + code + "</errorCode><message>" + e.getMessage() + "</message></error>";
        }
    }

    @GetMapping(value = "/rest/impressao/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Map<String, Boolean>> listarImpressoras(HttpServletResponse response) {
        try {
            LOGGER.debug("ImpressaoService: listarImpressoras");

            Map<String, Boolean> map = impressaoService.listarImpressoras();

            return new ResponseEntity<>(map, HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
