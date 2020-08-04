package com.b2wdigital.fazemu.presentation.web.service.rest;

import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 *
 * @author dailton.almeida
 */
public class ParametrosInfraRestServiceTest {

    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @InjectMocks
    private ParametrosInfraRestService instance;
    @Mock
    private ParametrosInfraRepository parametrosInfraRepository;
    private MockMvc mockMvc;
    private ObjectMapper om;

    public ParametrosInfraRestServiceTest() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(instance)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
        om = new ObjectMapper();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetAllAsMap200() throws Exception {
        Map<String, String> map = Maps.newTreeMap();
        map.put("a", "one");
        map.put("b", "two");
        when(parametrosInfraRepository.getAllAsMap()).thenReturn(map);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/parametrosInfra")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        verify(parametrosInfraRepository).getAllAsMap();
        verifyNoMoreInteractions(parametrosInfraRepository);
        Map<String, String> resultMap = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals(2, resultMap.size());
        assertEquals("one", resultMap.get("a"));
        assertEquals("two", resultMap.get("b"));
    }

    // @Test
    public void testGetAllAsMap422() throws Exception {
        String msg = "_msg";
        when(parametrosInfraRepository.getAllAsMap()).thenThrow(new FazemuServiceException(msg));

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/parametrosInfra")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        verify(parametrosInfraRepository).getAllAsMap();
        verifyNoMoreInteractions(parametrosInfraRepository);
        Map<String, String> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("422", map.get("errorCode"));
        assertEquals(msg, map.get("message"));
    }

    @Test
    public void testGetAsString200() throws Exception {
        when(parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, "a")).thenReturn("one");

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/parametrosInfra/a")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        verify(parametrosInfraRepository).getAsString(TIPO_DOCUMENTO_FISCAL_NFE, "a");
        verifyNoMoreInteractions(parametrosInfraRepository);
        Map<String, String> resultMap = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals(1, resultMap.size());
        assertEquals("one", resultMap.get("a"));
    }

    //@Test
    public void testGetAsString404() throws Exception {
        when(parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, "a")).thenReturn(null);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/parametrosInfra/a")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isNotFound())
                .andReturn();

        verify(parametrosInfraRepository).getAsString(TIPO_DOCUMENTO_FISCAL_NFE, "a");
        verifyNoMoreInteractions(parametrosInfraRepository);
        Map<String, String> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("404", map.get("errorCode"));
        assertEquals("Par\u00E2metro 'a' n\u00E3o encontrado.", map.get("message"));
    }

    //@Test
    public void testGetAsString422() throws Exception {
        String msg = "_msg";
        when(parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, "a")).thenThrow(new FazemuServiceException(msg));

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/parametrosInfra/a")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        verify(parametrosInfraRepository).getAsString(TIPO_DOCUMENTO_FISCAL_NFE, "a");
        verifyNoMoreInteractions(parametrosInfraRepository);
        Map<String, String> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("422", map.get("errorCode"));
        assertEquals(msg, map.get("message"));
    }

}
