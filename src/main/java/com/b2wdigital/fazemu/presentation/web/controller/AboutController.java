package com.b2wdigital.fazemu.presentation.web.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.ResponseBody;

@RestController
@RequestMapping(value = "/fazemu-web",produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class AboutController {
    private static final String DEFAULT_MAPPING = "/about";
    
    @GetMapping(value = DEFAULT_MAPPING)
    @ResponseBody
    public Map<String, Map<String, String>> about() throws IOException {
        ClassPathResource aboutPropertiesResource = new ClassPathResource("about.properties");
        Properties aboutProperties = new Properties();
        aboutProperties.load(aboutPropertiesResource.getInputStream());
        TreeMap<String, String> aboutMap = new TreeMap<>();
        aboutProperties.forEach((key, value) -> {
            aboutMap.put((String) key, (String) value);
        }); //converte properties para map

        TreeMap<String, Map<String, String>> result = new TreeMap<>(); //TreeMap mantem ordenado pela chave

        TreeMap<String, String> prop = new TreeMap<>();
        System.getProperties().forEach((key, value) -> {
            prop.put((String) key, (String) value);
        }); //converte hashtable para map
        
        result.put("aboutProperties", aboutMap);
        result.put("systemProperties", prop);
        result.put("environment", new TreeMap<>(System.getenv()));
        return result;
    }
    
}
