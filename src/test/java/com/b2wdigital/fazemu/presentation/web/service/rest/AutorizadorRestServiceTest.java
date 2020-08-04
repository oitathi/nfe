package com.b2wdigital.fazemu.presentation.web.service.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.b2wdigital.fazemu.business.repository.AutorizadorRepository;
import com.b2wdigital.fazemu.domain.Autorizador;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author dailton.almeida
 */
public class AutorizadorRestServiceTest {

    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @InjectMocks
    private AutorizadorRestService instance;
    @Mock
    private AutorizadorRepository autorizadorRepository;
    private MockMvc mockMvc;
    private ObjectMapper om;

    public AutorizadorRestServiceTest() {
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

    //@Test
    public void testListAtivos200() throws Exception {
        String tipoDocumentoFiscal = TIPO_DOCUMENTO_FISCAL_NFE;
        Autorizador autr1 = new Autorizador();
        autr1.setNome("_n1");
        Autorizador autr2 = new Autorizador();
        autr2.setNome("_n2");
        List<Autorizador> autrList = Arrays.asList(autr1, autr2);
        when(autorizadorRepository.listAtivosByTipoDocumentoFiscal(tipoDocumentoFiscal)).thenReturn(autrList);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/autorizador")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        verify(autorizadorRepository).listAtivosByTipoDocumentoFiscal(tipoDocumentoFiscal);
        verifyNoMoreInteractions(autorizadorRepository);
        Autorizador[] autorizadorArray = om.readValue(mvcReturn.getResponse().getContentAsString(), Autorizador[].class);
        assertEquals(2, autorizadorArray.length);
        assertEquals("_n1", autorizadorArray[0].getNome());
        assertEquals("_n2", autorizadorArray[1].getNome());
    }
//    @Test
//    public void testListAtivos422() throws Exception {
//        String msg = "_msg";
//        when(autorizadorRepository.listAtivos()).thenThrow(new FazemuServiceException(msg));
//
//        //call via mockMvc
//        MvcResult mvcReturn = mockMvc.perform(get("/autorizador")
//                .contentType(MediaType.APPLICATION_JSON)
//        )       //result
//                .andExpect(status().isInternalServerError())
//                .andReturn();
//
//        verify(autorizadorRepository).listAtivos();
//        verifyNoMoreInteractions(autorizadorRepository);
//        Map<String, Object> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
//        assertEquals("422", map.get("errorCode"));
//        assertEquals(msg, map.get("message"));
//    }

}
