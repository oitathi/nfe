package com.b2wdigital.fazemu.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.b2wdigital.fazemu.business.service.StorageService;

/**
 * Documento Clob Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class StorageServiceImpl implements StorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageServiceImpl.class);

    @Value("${fazemu.storage.endpoint.recover}")
    private String fazemuStorageRecoverEndpoint;

    @Override
    public String recoverFromStorage(String chaveAcesso, String tipoServico) {

        String url = fazemuStorageRecoverEndpoint + "?chaveAcesso=" + chaveAcesso + "&tipoServico=" + tipoServico;

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            LOGGER.error("Erro {} ao fazer recoverFromStorage url {} ", url, e.getMessage());
        }
        return null;
    }

}
