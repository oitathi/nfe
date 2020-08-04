package com.b2wdigital.fazemu.presentation.web.service.rest;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.b2wdigital.fazemu.business.service.RedisOperationsService;

@RestController
public class RedisOperationsRestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisOperationsService.class);
    
    @Autowired
    private RedisOperationsService redisOperationsService;
    
    @GetMapping(value = "/rest/addToSetAsStr", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void addToSetAsStr(HttpServletResponse response, @RequestParam(value = "key", required = true) String key, @RequestParam(value = "str", required = true) String str) throws Exception {
		LOGGER.debug("addToSetAsStr key {} str {}", key, str);
		redisOperationsService.addToSet(key, str);
    }
    
    @GetMapping(value = "/rest/addToSetAsLong", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void addToSetAsLong(HttpServletResponse response, @RequestParam(value = "key", required = true) String key, @RequestParam(value = "number", required = true) Long number) throws Exception {
		LOGGER.debug("addToSetAsLong key {} number {}", key, number);
		redisOperationsService.addToSet(key, number);
    }
    
    @GetMapping(value = "/rest/removeFromSetAsStr", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void removeFromSetAsStr(HttpServletResponse response, @RequestParam(value = "key", required = true) String key, @RequestParam(value = "str", required = true) String str) throws Exception {
		LOGGER.debug("removeFromSetAsStr key {} str {}", key, str);
		redisOperationsService.removeFromSet(key, str);
    }
    
    @GetMapping(value = "/rest/removeFromSetAsLong", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void removeFromSetAsLong(HttpServletResponse response, @RequestParam(value = "key", required = true) String key, @RequestParam(value = "number", required = true) Long number) throws Exception {
		LOGGER.debug("removeFromSetAsLong key {} number {}", key, number);
		redisOperationsService.removeFromSet(key, number);
    }
    
    @GetMapping(value = "/rest/members", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object members(HttpServletResponse response, @RequestParam(value = "key", required = true) String key) throws Exception {
		LOGGER.debug("members key {} ", key);
		return redisOperationsService.members(key);
    }
    
    @GetMapping(value = "/rest/expiresKey", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void expiresKey(HttpServletResponse response, @RequestParam(value = "key", required = true) String key) throws Exception {
		LOGGER.debug("members key {} ", key);
		redisOperationsService.expiresKey(key, 5L, TimeUnit.MILLISECONDS);
    }
    
    @GetMapping(value = "/rest/expiresAllKeys", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void expiresAllKeys(HttpServletResponse response) throws Exception {
                LOGGER.info("expiresAllKeys");
                Set<String> allKeys = redisOperationsService.allKeys();
                allKeys.forEach(key -> redisOperationsService.expiresKey(key, 5L, TimeUnit.MILLISECONDS));
    }
    
    @GetMapping(value = "/rest/difference", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object difference(HttpServletResponse response, @RequestParam(value = "key1", required = true) String key1, @RequestParam(value = "key2", required = true) String key2) throws Exception {
		LOGGER.debug("difference key1 {} key2 {}", key1, key2);
		return redisOperationsService.difference(key1, key2);
    }
    
    @GetMapping(value = "/rest/allKeys", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Set<String> allKeys(HttpServletResponse response) throws Exception {
		LOGGER.debug("allKeys");
		return redisOperationsService.allKeys();
    }
    
    @GetMapping(value = "/rest/getKeyValue", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object getKeyValue(HttpServletResponse response, @RequestParam(value = "key", required = true) String key) throws Exception {
		LOGGER.debug("getKeyValue key {}", key);
		return redisOperationsService.getKeyValue(key);
    }
    
    @GetMapping(value = "/rest/setKeyValue")
    public void setKeyValue(HttpServletResponse response, @RequestParam(value = "key", required = true) String key, @RequestParam(value = "value", required = true) String value) throws Exception {
		LOGGER.debug("setKeyValue key {} value {}", key, value);
		redisOperationsService.setKeyValue(key, value);
    }
    
}
