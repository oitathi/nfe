/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.presentation.web.service.rest;

import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoEventoRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoRetornoRepository;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.DocumentoRetorno;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.NotFoundException;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author dailton.almeida
 */
@RestController
public class DocumentoFiscalRestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentoFiscalRestService.class);

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private DocumentoEventoRepository documentoEventoRepository;

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoRetornoRepository documentoRetornoRepository;

    @GetMapping(value = "/nfe", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getXmlProcByChaveAcesso(@RequestParam(value = "chaveAcesso", required = true) String chaveAcesso) throws NotFoundException {
        LOGGER.debug("getXmlProcByChaveAcesso chaveAcesso {}", chaveAcesso);
        chaveAcesso = FazemuUtils.normalizarChaveAcesso(chaveAcesso);

        DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);

        String xmlProcessado = documentoClobRepository.getXmlRetornoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);

        if (xmlProcessado == null) {
            xmlProcessado = documentoClobRepository.getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);
        }

        if (xmlProcessado == null) {
            throw new NotFoundException("Chave " + chaveAcesso + " n\u00E3o encontrada.");
        }

        return xmlProcessado;
    }

    @GetMapping(value = "/nfe/{idXml}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getXmlById(@PathVariable("idXml") Long idXml) throws NotFoundException {
        LOGGER.debug("getXmlById idXml {}", idXml);
        DocumentoClob docl = documentoClobRepository.findById(idXml);
        LOGGER.debug("getXmlById idXml {} clob {}", idXml, docl == null ? null : docl.getClob());
        if (docl == null) {
            throw new NotFoundException("ID do XML " + idXml + " n\u00E3o encontrado.");
        }
        return docl.getClob();
    }

    @GetMapping(value = "/documentoFiscal", headers = {"Accept=" + MediaType.APPLICATION_JSON_VALUE}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public DocumentoFiscal findByChaveAcesso(@RequestParam(value = "chaveAcesso", required = true) String chaveAcesso) throws NotFoundException {
        LOGGER.debug("findByChaveAcesso chaveAcesso {}", chaveAcesso);
        chaveAcesso = FazemuUtils.normalizarChaveAcesso(chaveAcesso);
        DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);

        if (documentoFiscal == null) {
            throw new NotFoundException("Chave " + chaveAcesso + " n\u00E3o encontrada.");
        }
        return documentoFiscal;
    }

    @GetMapping(value = "/documentoFiscal/{idDocFiscal}/eventos", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<DocumentoEvento> findEventos(@PathVariable("idDocFiscal") Long idDocFiscal) {
        LOGGER.debug("findByIdDocFiscal idDocFiscal {}", idDocFiscal);
        return documentoEventoRepository.listByIdDocFiscal(idDocFiscal);
    }

    @GetMapping(value = "/documentoFiscal/{idDocFiscal}/retornos", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<DocumentoRetorno> findRetornos(@PathVariable("idDocFiscal") Long idDocFiscal) {
        LOGGER.debug("findByIdDocFiscal idDocFiscal {}", idDocFiscal);
        return documentoRetornoRepository.findByIdDocFiscal(idDocFiscal);
    }

}
