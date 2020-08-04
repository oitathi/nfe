package com.b2wdigital.fazemu.presentation.web.controller;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.b2wdigital.fazemu.business.service.EmissorRaizCertificadoDigitalService;
import com.b2wdigital.fazemu.domain.form.EmissorRaizCertificadoDigitalForm;
import com.b2wdigital.fazemu.utils.DateUtils;
import com.b2winc.corpserv.message.exception.NotFoundException;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
@RequestMapping(value = "/fazemu-web", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class EmissorRaizCertificadoDigitalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmissorRaizCertificadoDigitalController.class);
    private static final String DEFAULT_MAPPING = "/certificadoDigital";

    @Autowired
    private EmissorRaizCertificadoDigitalService emissorRaizCertificadoDigitalService;
    
    @GetMapping(value = DEFAULT_MAPPING )
    public List<EmissorRaizCertificadoDigitalForm> listByFiltros(@RequestParam Map<String, String> parameters) throws NotFoundException {
    	return emissorRaizCertificadoDigitalService.listByFiltros(parameters);
    }
   
    @GetMapping(value = DEFAULT_MAPPING + "/dataFimVigencia" )
    public List<EmissorRaizCertificadoDigitalForm> listByDataFimVigencia() throws NotFoundException {
    	return emissorRaizCertificadoDigitalService.listByDataFimVigencia();
    }

    @PostMapping(value = DEFAULT_MAPPING + "/adicionar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmissorRaizCertificadoDigitalForm insert(@RequestBody MultipartFile file, @RequestParam Date dataVigenciaInicio,
            @RequestParam String idEmissorRaiz, @RequestParam String usuario, @RequestParam String senha) throws Exception {
        try {
            LOGGER.info("insert form {}", file);

            EmissorRaizCertificadoDigitalForm form = new EmissorRaizCertificadoDigitalForm();
            form.setDataVigenciaInicio(DateUtils.convertDateToString(dataVigenciaInicio));
            form.setIdEmissorRaiz(idEmissorRaiz);
            form.setFileInput(file.getBytes());
            form.setUsuario(usuario);
            form.setSenha(senha);

            emissorRaizCertificadoDigitalService.insert(form);
            form.setMensagemRetorno("Emissor Raiz Certificado Digital incluído com sucesso");

            return form;
        } catch (IOException e) {
            throw new NotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    @PostMapping(value = DEFAULT_MAPPING + "/atualizar")
    public EmissorRaizCertificadoDigitalForm update(@RequestBody EmissorRaizCertificadoDigitalForm form) throws Exception {
        try {
            LOGGER.info("update form {}", form);

            emissorRaizCertificadoDigitalService.update(form);
            form.setMensagemRetorno("Emissor Raiz Certificado Digital atualizado com sucesso");

            return form;
        } catch (Exception e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    

}
