package com.b2wdigital.fazemu.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class PrintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrintService.class);

    @Value("${fazemu.cups.endpoint.print}")
    private String fazemuCupsEndpoint;

    public void print(byte[] file, String impressora, String nomeArquivo) throws HttpClientErrorException {

        LOGGER.info("Printing...");
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file", new ByteArrayResource(file) {
            @Override
            public String getFilename() {
                return nomeArquivo;
            }
        });
        map.add("impressora", impressora);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange(fazemuCupsEndpoint, HttpMethod.POST, requestEntity, String.class);
    }
}
