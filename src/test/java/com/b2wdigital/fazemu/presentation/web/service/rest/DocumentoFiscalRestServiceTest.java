/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.presentation.web.service.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author dailton.almeida
 */
public class DocumentoFiscalRestServiceTest {

    @InjectMocks
    private DocumentoFiscalRestService instance;
    @Mock
    private DocumentoFiscalRepository documentoFiscalRepository;
    @Mock
    private DocumentoClobRepository documentoClobRepository;
    private MockMvc mockMvc;
    private ObjectMapper om;
    private final String chaveAcesso = "_chaveacesso";
    private final String xml = "_xml";

    public DocumentoFiscalRestServiceTest() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(instance)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                //                .addFilter(new ExceptionHandlerFilter())
                .build();
        om = new ObjectMapper();
    }

    @After
    public void tearDown() {
    }

    //@Test
    public void testGetXmlProcByChaveAcesso200() throws Exception {
        when(documentoClobRepository.getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscalRepository.findByChaveAcesso(chaveAcesso).getId(), TipoServicoEnum.AUTORIZACAO)).thenReturn(xml);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/nfe?chaveAcesso={chaveAcesso}", chaveAcesso)
                .contentType(MediaType.TEXT_PLAIN)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        verify(documentoClobRepository).getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscalRepository.findByChaveAcesso(chaveAcesso).getId(), TipoServicoEnum.AUTORIZACAO);
        verifyNoMoreInteractions(documentoFiscalRepository);
        verifyNoMoreInteractions(documentoClobRepository);
        String strResult = mvcReturn.getResponse().getContentAsString();
        assertEquals(xml, strResult);
    }

    //@Test
    public void testGetXmlProcByChaveAcesso404() throws Exception {
        when(documentoClobRepository.getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscalRepository.findByChaveAcesso(chaveAcesso).getId(), TipoServicoEnum.AUTORIZACAO)).thenReturn(null);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/nfe?chaveAcesso={chaveAcesso}", chaveAcesso)
                .contentType(MediaType.TEXT_PLAIN)
        ) //result
                .andExpect(status().isNotFound())
                .andReturn();

        verify(documentoClobRepository).getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscalRepository.findByChaveAcesso(chaveAcesso).getId(), TipoServicoEnum.AUTORIZACAO);
        verifyNoMoreInteractions(documentoFiscalRepository);
        verifyNoMoreInteractions(documentoClobRepository);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("404", map.get("errorCode"));
        assertEquals("Chave _chaveacesso n\u00E3o encontrada.", map.get("message"));
    }

    // @Test
    public void testGetXmlProcByChaveAcesso422() throws Exception {
        String msg = "_msg";
        when(documentoClobRepository.getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscalRepository.findByChaveAcesso(chaveAcesso).getId(), TipoServicoEnum.AUTORIZACAO)).thenThrow(new FazemuServiceException(msg));

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/nfe?chaveAcesso={chaveAcesso}", chaveAcesso)
                .contentType(MediaType.TEXT_PLAIN)
        ) //result
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        verify(documentoClobRepository).getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscalRepository.findByChaveAcesso(chaveAcesso).getId(), TipoServicoEnum.AUTORIZACAO);
        verifyNoMoreInteractions(documentoFiscalRepository);
        verifyNoMoreInteractions(documentoClobRepository);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("422", map.get("errorCode"));
        assertEquals(msg, map.get("message"));
    }

    //@Test
    public void testGetXmlProcByChaveAcessoGenericException() throws Exception {
        String msg = "_msg";
        when(documentoClobRepository.getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscalRepository.findByChaveAcesso(chaveAcesso).getId(), TipoServicoEnum.AUTORIZACAO)).thenThrow(new RuntimeException(msg));

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/nfe?chaveAcesso={chaveAcesso}", chaveAcesso)
                .contentType(MediaType.TEXT_PLAIN)
        ) //result
                .andExpect(status().isInternalServerError())
                .andReturn();

        verify(documentoClobRepository).getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscalRepository.findByChaveAcesso(chaveAcesso).getId(), TipoServicoEnum.AUTORIZACAO);
        verifyNoMoreInteractions(documentoFiscalRepository);
        verifyNoMoreInteractions(documentoClobRepository);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("500", map.get("errorCode"));
        assertEquals(msg, map.get("message"));
    }

}
