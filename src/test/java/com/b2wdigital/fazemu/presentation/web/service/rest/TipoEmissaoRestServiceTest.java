package com.b2wdigital.fazemu.presentation.web.service.rest;

import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.repository.TipoEmissaoRepository;
import com.b2wdigital.fazemu.domain.TipoEmissao;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
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
public class TipoEmissaoRestServiceTest {

    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @InjectMocks
    private TipoEmissaoRestService instance;
    @Mock
    private ParametrosInfraRepository parametrosInfraRepository;
    @Mock
    private TipoEmissaoRepository tipoEmissaoRepository;
    private MockMvc mockMvc;
    private ObjectMapper om;
    private final Long tipoEmissao = 2L;
    private final Long tipoEmissaoDefault = 5L;
    private final Long idEstado = 3L;

    public TipoEmissaoRestServiceTest() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(instance)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
        om = new ObjectMapper();
        when(parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TP_EMISSAO)).thenReturn(tipoEmissaoDefault.intValue());
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testListAtivos() throws Exception {
        TipoEmissao tpem1 = new TipoEmissao();
        tpem1.setNome("_n1");
        TipoEmissao tpem2 = new TipoEmissao();
        tpem2.setNome("_n2");
        List<TipoEmissao> tipoEmissaos = Arrays.asList(tpem1, tpem2);
        when(tipoEmissaoRepository.listAtivos()).thenReturn(tipoEmissaos);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/tipoEmissao")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        verify(tipoEmissaoRepository).listAtivos();
        verifyNoMoreInteractions(tipoEmissaoRepository);
        TipoEmissao[] tipoEmissaoArray = om.readValue(mvcReturn.getResponse().getContentAsString(), TipoEmissao[].class);
        assertEquals(2, tipoEmissaoArray.length);
        assertEquals("_n1", tipoEmissaoArray[0].getNome());
        assertEquals("_n2", tipoEmissaoArray[1].getNome());
    }

    @Test
    public void testFindById200() throws Exception {
        TipoEmissao tpem = new TipoEmissao();
        tpem.setNome("_nome");
        when(tipoEmissaoRepository.findById(tipoEmissao)).thenReturn(tpem);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/tipoEmissao/{tipoEmissao}", tipoEmissao)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        verify(tipoEmissaoRepository).findById(tipoEmissao);
        verifyNoMoreInteractions(tipoEmissaoRepository);
        TipoEmissao tipoEmissao = om.readValue(mvcReturn.getResponse().getContentAsString(), TipoEmissao.class);
        assertEquals("_nome", tipoEmissao.getNome());
    }

    //@Test
    public void testFindById404() throws Exception {
        TipoEmissao tpem = new TipoEmissao();
        tpem.setNome("_nome");
        when(tipoEmissaoRepository.findById(tipoEmissao)).thenReturn(null);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/tipoEmissao/{tipoEmissao}", tipoEmissao)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ) //result
                .andExpect(status().isNotFound())
                .andReturn();

        verify(tipoEmissaoRepository).findById(tipoEmissao);
        verifyNoMoreInteractions(tipoEmissaoRepository);
        Map<String, Object> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("404", map.get("errorCode"));
        assertEquals("Tipo de emiss\u00E3o 2 n\u00E3o encontrado.", map.get("message"));
    }

    //@Test
    public void testFindById422() throws Exception {
        String msg = "_msg";
        when(tipoEmissaoRepository.findById(tipoEmissao)).thenThrow(new FazemuServiceException(msg));

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/tipoEmissao/{tipoEmissao}", tipoEmissao)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ) //result
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        verify(tipoEmissaoRepository).findById(tipoEmissao);
        verifyNoMoreInteractions(tipoEmissaoRepository);
        Map<String, Object> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("422", map.get("errorCode"));
        assertEquals(msg, map.get("message"));
    }

    @Test
    public void testFindByIdEstado200() throws Exception {
        TipoEmissao tpem = new TipoEmissao();
        tpem.setNome("_nome");
        when(tipoEmissaoRepository.findByIdEstado(idEstado)).thenReturn(tpem);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/tipoEmissao?idEstado={idEstado}", idEstado)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        verify(tipoEmissaoRepository).findByIdEstado(idEstado);
        verifyNoMoreInteractions(tipoEmissaoRepository);
        TipoEmissao tipoEmissao = om.readValue(mvcReturn.getResponse().getContentAsString(), TipoEmissao.class);
        assertEquals("_nome", tipoEmissao.getNome());
    }

    //@Test
    public void testFindByIdEstado404() throws Exception {
        TipoEmissao tpem = new TipoEmissao();
        tpem.setNome("_nome");
        when(tipoEmissaoRepository.findByIdEstado(idEstado)).thenReturn(null);
        when(tipoEmissaoRepository.findById(tipoEmissaoDefault)).thenReturn(null);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/tipoEmissao?idEstado={idEstado}", idEstado)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ) //result
                .andExpect(status().isNotFound())
                .andReturn();

        verify(tipoEmissaoRepository).findByIdEstado(idEstado);
        verify(tipoEmissaoRepository).findById(tipoEmissaoDefault);
        verifyNoMoreInteractions(tipoEmissaoRepository);
        Map<String, Object> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("404", map.get("errorCode"));
        assertEquals("Tipo de emiss\u00E3o 5 n\u00E3o encontrado.", map.get("message"));
    }

    //@Test
    public void testFindByIdEstado422() throws Exception {
        String msg = "_msg";
        when(tipoEmissaoRepository.findByIdEstado(idEstado)).thenThrow(new FazemuServiceException(msg));

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/tipoEmissao?idEstado={idEstado}", idEstado)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ) //result
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        verify(tipoEmissaoRepository).findByIdEstado(idEstado);
        verifyNoMoreInteractions(tipoEmissaoRepository);
        Map<String, Object> map = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals("422", map.get("errorCode"));
        assertEquals(msg, map.get("message"));
    }

}
