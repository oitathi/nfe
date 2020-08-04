/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.presentation.web.service.rest;

import com.b2wdigital.fazemu.business.service.ConsultarRetornoService;
import com.b2wdigital.fazemu.domain.form.ConsultarRetornoForm;
import com.b2wdigital.fazemu.exception.NotFoundException;
import com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4.TProtNFe.InfProt;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 *
 * @author dailton.almeida
 */
public class ConsultarRetornoRestServiceTest {
    @InjectMocks private ConsultarRetornoRestService instance;
    @Mock private ConsultarRetornoService consultarRetornoService;
    private MockMvc mockMvc;
    private ObjectMapper om;
    private final String chaveAcesso = "_ca";
    private final String message = "_msg";

    public ConsultarRetornoRestServiceTest() {
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
    public void testConsultarRetornoXML200() throws Exception {
        ConsultarRetornoForm form = new ConsultarRetornoForm();
        form.setChaveAcesso(chaveAcesso);
        String expResult = "_expresult";
        
        when(consultarRetornoService.consultarRetornoAsString(form)).thenReturn(expResult);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/rest/consultarretorno?chaveAcesso={chaveAcesso}", chaveAcesso)
                .accept(MediaType.TEXT_PLAIN)
        )       //result
                .andExpect(status().isOk())
                .andReturn();

        verify(consultarRetornoService).consultarRetornoAsString(form);
        verifyNoMoreInteractions(consultarRetornoService);
        assertEquals(expResult, mvcReturn.getResponse().getContentAsString());
    }
    @Test
    public void testConsultarRetornoXML404() throws Exception {
        ConsultarRetornoForm form = new ConsultarRetornoForm();
        form.setChaveAcesso(chaveAcesso);
        String expResult = "<error><errorCode>404</errorCode><message>_msg</message></error>";
        
        when(consultarRetornoService.consultarRetornoAsString(form)).thenThrow(new NotFoundException(message));

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/rest/consultarretorno?chaveAcesso={chaveAcesso}", chaveAcesso)
                .accept(MediaType.TEXT_PLAIN)
        )       //result
                .andExpect(status().isNotFound())
                .andReturn();

        verify(consultarRetornoService).consultarRetornoAsString(form);
        verifyNoMoreInteractions(consultarRetornoService);
        assertEquals(expResult, mvcReturn.getResponse().getContentAsString());
    }
    @Test
    public void testConsultarRetornoXML500() throws Exception {
        ConsultarRetornoForm form = new ConsultarRetornoForm();
        form.setChaveAcesso(chaveAcesso);
        String expResult = "<error><errorCode>500</errorCode><message>_msg</message></error>";
        
        when(consultarRetornoService.consultarRetornoAsString(form)).thenThrow(new RuntimeException(message));

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/rest/consultarretorno?chaveAcesso={chaveAcesso}", chaveAcesso)
                .accept(MediaType.TEXT_PLAIN)
        )       //result
                .andExpect(status().isInternalServerError())
                .andReturn();

        verify(consultarRetornoService).consultarRetornoAsString(form);
        verifyNoMoreInteractions(consultarRetornoService);
        assertEquals(expResult, mvcReturn.getResponse().getContentAsString());
    }

    @Test
    public void testConsultarRetornoJSON200() throws Exception {
        ConsultarRetornoForm form = new ConsultarRetornoForm();
        form.setChaveAcesso(chaveAcesso);
        InfProt infProt = new InfProt();
        infProt.setXMotivo("_xmotivo");
        
        when(consultarRetornoService.consultarRetornoAsObject(form)).thenReturn(infProt);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/rest/consultarretorno?chaveAcesso={chaveAcesso}", chaveAcesso)
                .accept(MediaType.APPLICATION_JSON)
        )       //result
                .andExpect(status().isOk())
                .andReturn();

        verify(consultarRetornoService).consultarRetornoAsObject(form);
        verifyNoMoreInteractions(consultarRetornoService);
        Map<String, Object> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("_xmotivo", map.get("xmotivo"));
    }
    //@Test
    public void testConsultarRetornoJSON404() throws Exception {
        ConsultarRetornoForm form = new ConsultarRetornoForm();
        form.setChaveAcesso(chaveAcesso);
        
        when(consultarRetornoService.consultarRetornoAsObject(form)).thenThrow(new NotFoundException(message));

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/rest/consultarretorno?chaveAcesso={chaveAcesso}", chaveAcesso)
                .accept(MediaType.APPLICATION_JSON)
        )       //result
                .andExpect(status().isNotFound())
                .andReturn();

        verify(consultarRetornoService).consultarRetornoAsObject(form);
        verifyNoMoreInteractions(consultarRetornoService);
        Map<String, Object> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("404", map.get("errorCode"));
        assertEquals(message, map.get("message"));
    }
    //@Test
    public void testConsultarRetornoJSON500() throws Exception {
        ConsultarRetornoForm form = new ConsultarRetornoForm();
        form.setChaveAcesso(chaveAcesso);
        
        when(consultarRetornoService.consultarRetornoAsObject(form)).thenThrow(new RuntimeException(message));

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/rest/consultarretorno?chaveAcesso={chaveAcesso}", chaveAcesso)
                .accept(MediaType.APPLICATION_JSON)
        )       //result
                .andExpect(status().isInternalServerError())
                .andReturn();

        verify(consultarRetornoService).consultarRetornoAsObject(form);
        verifyNoMoreInteractions(consultarRetornoService);
        Map<String, Object> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("500", map.get("errorCode"));
        assertEquals(message, map.get("message"));
    }
    
}
