package com.b2wdigital.fazemu.service.impl;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.FecharEnviarLoteService;

/**
 *
 * @author dailton.almeida
 */
public class EmitirLoteServiceImplTest {
    @InjectMocks private EmitirLoteServiceImpl instance;
    @Mock private FecharEnviarLoteService fecharEnviarLoteService;
    @Mock private CacheLoteRepository cacheLoteRepository;
    @Mock private ParametrosInfraRepository parametrosInfraRepository;

    public EmitirLoteServiceImplTest() {
    }
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testGetAsIntegerContains() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "23");
        int result = instance.getAsInteger(map, "key", 29);
        assertEquals(23, result);
    }
    @Test
    public void testGetAsIntegerDoesNotContain() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "23");
        int result = instance.getAsInteger(map, "anotherKey", 29);
        assertEquals(29, result);
    }
}
