package com.b2wdigital.fazemu.presentation.web.service.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.b2wdigital.fazemu.business.repository.CodigoRetornoAutorizadorRepository;
import com.b2wdigital.fazemu.business.service.CodigoRetornoAutorizadorService;
import com.b2wdigital.fazemu.domain.form.CodigoRetornoAutorizadorForm;
import com.b2wdigital.fazemu.presentation.web.controller.CodigoRetornoAutorizadorController;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author dailton.almeida
 */
public class CodigoRetornoAutorizadorRestServiceTest {
    @InjectMocks private CodigoRetornoAutorizadorController instance;
    @Mock private CodigoRetornoAutorizadorService codigoRetornoAutorizadorService;
    @Mock private CodigoRetornoAutorizadorRepository codigoRetornoAutorizadorRepository;
    private MockMvc mockMvc;
    private ObjectMapper om;
    private final Integer cStat = 1;
    private final CodigoRetornoAutorizadorForm crau = CodigoRetornoAutorizadorForm.build(cStat.toString());
    private final List<CodigoRetornoAutorizadorForm> listCrau = Lists.newArrayList();
    
    public CodigoRetornoAutorizadorRestServiceTest() {
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
    public void testListByCodigo200() throws Exception {
    	listCrau.add(crau);
        when(codigoRetornoAutorizadorService.listByCodigo(cStat)).thenReturn(listCrau);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/fazemu-web/codigoRetornoAutorizador/{id}", cStat)
                .contentType(MediaType.TEXT_PLAIN)
        )       //result
                .andExpect(status().isOk())
                .andReturn();

        verify(codigoRetornoAutorizadorService).listByCodigo(cStat);
        verifyNoMoreInteractions(codigoRetornoAutorizadorService);
        List<CodigoRetornoAutorizadorForm> list = om.readValue(mvcReturn.getResponse().getContentAsString(), new TypeReference<List<CodigoRetornoAutorizadorForm>>() {});
        assertEquals(crau.getId(), list.iterator().next().getId());
    }

   // @Test
    public void testFindById404() throws Exception {
        when(codigoRetornoAutorizadorRepository.findById(cStat)).thenReturn(null);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/codigoRetornoAutorizador/{id}", cStat)
                .contentType(MediaType.TEXT_PLAIN)
        )       //result
                .andExpect(status().isNotFound())
                .andReturn();

        verify(codigoRetornoAutorizadorRepository).findById(cStat);
        verifyNoMoreInteractions(codigoRetornoAutorizadorRepository);
        Map<?, ?> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("404", map.get("errorCode"));
        assertEquals("C\u00F3digo de retorno 1 n\u00E3o encontrado.", map.get("message"));
    }

    //@Test
    public void testFindByIdGenericException() throws Exception {
        String msg = "_msg";
        when(codigoRetornoAutorizadorRepository.findById(cStat)).thenThrow(new RuntimeException(msg));

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/codigoRetornoAutorizador/{id}", cStat)
                .contentType(MediaType.TEXT_PLAIN)
        )       //result
                .andExpect(status().isInternalServerError())
                .andReturn();

        verify(codigoRetornoAutorizadorRepository).findById(cStat);
        verifyNoMoreInteractions(codigoRetornoAutorizadorRepository);
        Map<?, ?> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("500", map.get("errorCode"));
        assertEquals(msg, map.get("message"));
    }
    
}
