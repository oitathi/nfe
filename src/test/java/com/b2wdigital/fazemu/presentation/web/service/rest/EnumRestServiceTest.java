package com.b2wdigital.fazemu.presentation.web.service.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author dailton.almeida
 */
public class EnumRestServiceTest {

    @InjectMocks
    private EnumRestService instance;
    private MockMvc mockMvc;
    private ObjectMapper om;

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

    @Test
    public void testPontoDocumento() throws Exception {
        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/enum/pontodocumento")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals(18, map.size());
        assertEquals("Documento Raw Recebido", map.get("DRAW"));
        assertEquals("Documento Assinado Recebido", map.get("DASS"));
        assertEquals("Documento de Layout Recebido", map.get("DLAY"));
        assertEquals("Documento Epec Recebido", map.get("DEPEC"));
        assertEquals("Documento Fiscal Criado via Manifestação", map.get("DAUTM"));
        assertEquals("Documento de Recibo", map.get("DRECI"));
        assertEquals("Documento Em Consulta de Recibo", map.get("DCORE"));
        assertEquals("Documento de Layout Processado", map.get("DPLAY"));
        assertEquals("Documento Processado", map.get("DPROC"));
        assertEquals("Documento Enviado para Normal Após Epec", map.get("DENVI"));
        assertEquals("Documento Cancelamento Recebido", map.get("DCANC"));
        assertEquals("Documento Carta de Correção Recebida", map.get("DCCOR"));
        assertEquals("Documento Manifestação Recebido", map.get("DMANF"));
        assertEquals("Documento de Resumo NFe", map.get("DRES"));
        assertEquals("Documento Inutilização Recebido", map.get("DINUT"));
        assertEquals("Erro ao Emitir Documento", map.get("ERRE"));
        assertEquals("Erro ao Consultar Recibo Documento", map.get("ERRR"));
        assertEquals("Erro ao Emitir Documento", map.get("ERRC"));
    }

    @Test
    public void testSituacaoLoteAsMap() throws Exception {
        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/enum/situacaolote?type=mapa")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals(6, map.size());
        assertEquals("Aberto", map.get("A"));
        assertEquals("Fechado", map.get("F"));
        assertEquals("Erro", map.get("E"));
        assertEquals("Enviado", map.get("V"));
        assertEquals("Liquidado", map.get("L"));
        assertEquals("Cancelado", map.get("C"));
    }

    @Test
    public void testSituacaoLoteAsCodigos() throws Exception {
        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/enum/situacaolote?type=codigos")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        String[] a = om.readValue(mvcReturn.getResponse().getContentAsString(), String[].class);
        assertEquals(6, a.length);
        assertEquals("A", a[0]);
        assertEquals("F", a[1]);
        assertEquals("E", a[2]);
        assertEquals("V", a[3]);
        assertEquals("L", a[4]);
        assertEquals("C", a[5]);
    }

    @Test
    public void testSituacaoLoteAsDescricoes() throws Exception {
        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/enum/situacaolote?type=descricoes")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        String[] a = om.readValue(mvcReturn.getResponse().getContentAsString(), String[].class);
        assertEquals(6, a.length);
        assertEquals("Aberto", a[0]);
        assertEquals("Fechado", a[1]);
        assertEquals("Erro", a[2]);
        assertEquals("Enviado", a[3]);
        assertEquals("Liquidado", a[4]);
        assertEquals("Cancelado", a[5]);
    }

    @Test
    public void testDocumentoRetorno() throws Exception {
        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/enum/documentoretorno")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals(5, map.size());
        assertEquals("Autoriza\u00E7\u00E3o", map.get("AUTR"));
        assertEquals("Cancelamento", map.get("CANC"));
    }

}
