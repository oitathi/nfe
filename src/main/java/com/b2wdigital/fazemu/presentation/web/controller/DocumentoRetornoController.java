package com.b2wdigital.fazemu.presentation.web.controller;

import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.service.DocumentoRetornoService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.domain.DocumentoRetorno;
import com.b2wdigital.fazemu.exception.NotFoundException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestParam;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class DocumentoRetornoController {

    private static final String DEFAULT_MAPPING = "/documentoRetorno";

    @Autowired
    private DocumentoRetornoService documentoRetornoService;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @GetMapping(value = DEFAULT_MAPPING)
    public List<DocumentoRetorno> findByIdDocFiscal(@RequestParam(value = "idDocFiscal", required = false) Long idDocFiscal) {
        List<DocumentoRetorno> retorno = documentoRetornoService.findByIdDocFiscal(idDocFiscal);
        return retorno;
    }

    @GetMapping(value = DEFAULT_MAPPING + "/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getXmlById(@RequestParam(value = "idXml", required = true) Long idXml) throws Exception {
        DocumentoClob docl = documentoClobRepository.findById(idXml);

        if (docl == null) {
            throw new NotFoundException("ID do XML " + idXml + " n\u00E3o encontrado.");
        }

        return docl.getClob();
    }

}
