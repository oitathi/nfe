package com.b2wdigital.fazemu.presentation.web.service.rest;

import com.b2wdigital.fazemu.business.service.ConsultarRetornoService;
import com.b2wdigital.fazemu.domain.form.ConsultarRetornoForm;
import com.b2wdigital.fazemu.exception.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsultarRetornoRestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarRetornoRestService.class);
    private static final String DEFAULT_MAPPING = "/rest/consultarretorno";

    @Autowired
    private ConsultarRetornoService consultarRetornoService;

    @GetMapping(value = DEFAULT_MAPPING, headers = {"Accept=*/*", "Accept=" + MediaType.TEXT_XML_VALUE}, produces = MediaType.TEXT_PLAIN_VALUE)
    public String consultarRetornoXML(HttpServletResponse response, ConsultarRetornoForm form) {
        try {
            LOGGER.debug("consultarRetornoXML form {}", form);
            return consultarRetornoService.consultarRetornoAsString(form);
        } catch (NotFoundException e) {
            int code = HttpStatus.NOT_FOUND.value();
            response.setStatus(code);
            return "<error><errorCode>" + code + "</errorCode><message>" + e.getMessage() + "</message></error>";
        } catch (Exception e) {
            int code = HttpStatus.INTERNAL_SERVER_ERROR.value();
            response.setStatus(code);
            return "<error><errorCode>" + code + "</errorCode><message>" + e.getMessage() + "</message></error>";
        }
    }

    @GetMapping(value = DEFAULT_MAPPING, headers = {"Accept=" + MediaType.APPLICATION_JSON_VALUE}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object consultarRetornoJSON(HttpServletResponse response, ConsultarRetornoForm form) throws Exception {
        LOGGER.debug("consultarRetornoJSON form {}", form);
        return consultarRetornoService.consultarRetornoAsObject(form);
    }

}
