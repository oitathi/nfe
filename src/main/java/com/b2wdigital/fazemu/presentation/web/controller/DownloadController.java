package com.b2wdigital.fazemu.presentation.web.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.b2wdigital.fazemu.business.service.DanfeService;
import com.b2wdigital.fazemu.business.service.DocumentoClobService;
import com.b2wdigital.fazemu.business.service.DocumentoFiscalService;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.form.DocumentoFiscalForm;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.utils.CsvWritter;
import com.b2wdigital.fazemu.utils.TxtReader;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.itextpdf.text.Document;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web")
public class DownloadController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadController.class);
    private static final String DEFAULT_MAPPING = "/download";

    @Autowired
    private DocumentoFiscalService documentoFiscalService;

    @Autowired
    private DocumentoClobService documentoClobService;

    @Autowired
    private DanfeService danfeService;

    @PostMapping(value = DEFAULT_MAPPING + "/processar/danfe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] downloadDanfe(HttpServletResponse response, @RequestBody MultipartFile file,
            @RequestHeader(value = "Content-Transfer-Encoding", required = false) String contentTransferEncoding) throws Exception {
        LOGGER.debug("DownloadController: processar/danfe");

        ByteArrayInputStream bais = new ByteArrayInputStream(file.getBytes());

        // Monta lista de chaves de acesso baseado no arquivo carregado
        Set<String> listaChavesAcesso = TxtReader.getListaChaveAcesso(bais);

        // Cria arquivo zip e adiciona entradas
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zout = new ZipOutputStream(baos)) {
            for (String chaveAcesso : listaChavesAcesso) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                LOGGER.info("Gerando PDF para chave de acesso {} ", chaveAcesso);

                Document document = danfeService.fromChaveAcessoToPDF(chaveAcesso, bos);

                if (document != null) {
                    ZipEntry zip = new ZipEntry("NFe" + chaveAcesso + ".pdf");
                    zout.putNextEntry(zip);
                    zout.write(bos.toByteArray());
                    zout.closeEntry();
                }
            }
        }

        // Prepare response
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("application/zip");

        try {
            if (StringUtils.equalsIgnoreCase(contentTransferEncoding, "base64")) {
                IOUtils.write(Base64.getEncoder().encode(baos.toByteArray()), response.getOutputStream());
                return Base64.getEncoder().encode(baos.toByteArray());
            } else {
                IOUtils.write(baos.toByteArray(), response.getOutputStream());
                return baos.toByteArray();
            }
        } finally {
            response.getOutputStream().flush();
            response.getOutputStream().close();

            baos.flush();
            baos.close();
        }

    }

    @RequestMapping(value = DEFAULT_MAPPING + "/processar/csv", method = RequestMethod.POST)
    public byte[] downloadCSV(@RequestBody List<DocumentoFiscalForm> pesquisa) {
        List<String[]> pesquisaArr = new ArrayList<>();
        pesquisaArr.add(DocumentoFiscalForm.getDocumentoFiscalHeader());
        pesquisa.forEach(dff -> pesquisaArr.add(dff.formToArr()));

        CsvWritter writter = new CsvWritter();
        byte[] arr = writter.createCsvArrBytes(pesquisaArr);
        return arr;
    }

    @PostMapping(value = DEFAULT_MAPPING + "/processar/xml", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] downloadXML(HttpServletResponse response, @RequestBody MultipartFile file,
            @RequestHeader(value = "Content-Transfer-Encoding", required = false) String contentTransferEncoding) throws Exception {

        LOGGER.debug("DownloadController: processar/xml");

        ByteArrayInputStream bais = new ByteArrayInputStream(file.getBytes());

        // Monta lista de chaves de acesso baseado no arquivo carregado
        Set<String> listaChavesAcesso = TxtReader.getListaChaveAcesso(bais);

        // Cria arquivo zip e adiciona entradas
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zout = new ZipOutputStream(baos)) {
            for (String chaveAcesso : listaChavesAcesso) {
                LOGGER.info("Gerando XML para chave de acesso {} ", chaveAcesso);

                DocumentoFiscal documentoFiscal = documentoFiscalService.findByChaveAcesso(chaveAcesso);

                String xmlNfeProc = documentoClobService.getXmlRetornoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);

                if (xmlNfeProc != null) {
                    org.w3c.dom.Document document = XMLUtils.convertStringToDocument(xmlNfeProc);
                    document.setXmlStandalone(true);
                    XMLUtils.cleanNameSpace(document);

                    String xmlFinal = XMLUtils.convertDocumentToString(document);

                    ZipEntry zip = new ZipEntry("NFe" + chaveAcesso + ".xml");
                    zout.putNextEntry(zip);
                    zout.write(xmlFinal.getBytes());
                    zout.closeEntry();
                }
            }
        }

        // Prepare response
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("application/zip");

        try {
            if (StringUtils.equalsIgnoreCase(contentTransferEncoding, "base64")) {
                IOUtils.write(Base64.getEncoder().encode(baos.toByteArray()), response.getOutputStream());
                return Base64.getEncoder().encode(baos.toByteArray());
            } else {
                IOUtils.write(baos.toByteArray(), response.getOutputStream());
                return baos.toByteArray();
            }
        } finally {
            response.getOutputStream().flush();
            response.getOutputStream().close();

            baos.flush();
            baos.close();
        }

    }

}
