package com.b2wdigital.fazemu.presentation.web.service.rest;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import com.b2wdigital.fazemu.business.service.ImpressaoNFeService;
import com.b2wdigital.fazemu.domain.form.ImpressaoNFeForm;

@RestController
public class ImpressaoNFeRestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImpressaoNFeRestService.class);

    @Autowired
    private ImpressaoNFeService impressaoNFeService;

    @GetMapping(value = "/rest/impressao", headers = {"Accept=*/*", "Accept=" + MediaType.TEXT_XML_VALUE}, produces = MediaType.TEXT_PLAIN_VALUE)
    public String impressaoComNFe(HttpServletResponse response, ImpressaoNFeForm form) {
        try {
            LOGGER.debug("ImpressaoNFeService: impressaoComNFe form {}", form);

            impressaoNFeService.imprimirComNFe(form);

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

    @PostMapping(value = "/rest/impressao", consumes = MediaType.TEXT_XML_VALUE, headers = {"Accept=*/*", "Accept=" + MediaType.TEXT_XML_VALUE}, produces = MediaType.TEXT_PLAIN_VALUE)
    public String impressaoSemNFe(HttpServletResponse response, @RequestBody String nfe) {
        try {
            LOGGER.debug("ImpressaoNFeService: impressaoSemNFe");

            impressaoNFeService.imprimirSemNFe(nfe);

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

//	@PostMapping(value = "/rest/impressaoEPEC",consumes = MediaType.TEXT_XML_VALUE, headers = { "Accept=*/*", "Accept=" + MediaType.TEXT_XML_VALUE }, produces = MediaType.TEXT_PLAIN_VALUE)
//	public String impressaoEPEC(HttpServletResponse response, @RequestBody String nfe) {
//		try {
//			LOGGER.debug("ImpressaoNFeService: impressaoEPEC");
//
//			impressaoNFeService.imprimirEPEC(nfe);
//			
//			int code = HttpStatus.OK.value();
//			response.setStatus(code);
//			return "<fazemu><code>" + code + "</code><message>SUCESSO</message></fazemu>";
//		} catch (HttpClientErrorException.NotFound |  HttpClientErrorException.BadRequest e) {
//			int code = e.getRawStatusCode();
//			response.setStatus(code);
//			return "<error><errorCode>" + code + "</errorCode><message>" + e.getMessage() + "</message></error>";
//		} catch (Exception e) {
//			int code = HttpStatus.INTERNAL_SERVER_ERROR.value();
//			response.setStatus(code);
//			return "<error><errorCode>" + code + "</errorCode><message>" + e.getMessage() + "</message></error>";
//		}
//	}
}
