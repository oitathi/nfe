package com.b2wdigital.fazemu.presentation.web.service.rest;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.Map;
import java.util.stream.LongStream;
import org.assertj.core.util.Sets;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 *
 * @author dailton.almeida
 */
public class LoteRestServiceTest {

    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @InjectMocks
    private LoteRestService instance;
    @Mock
    private CacheLoteRepository cacheLoteRepository;
    @Mock
    private EstadoRepository estadoRepository;
    @Mock
    private ParametrosInfraRepository parametrosInfraRepository;
    private MockMvc mockMvc;
    private ObjectMapper om;
    private Estado sp, rj;
    private ResumoLote lote1, lote2;
    private final int ibgeSP = 35;
    private final int ibgeRJ = 33;
    private final String sFalse = Boolean.FALSE.toString();
    private final String sTrue = Boolean.TRUE.toString();

    public LoteRestServiceTest() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(instance).build();
        om = new ObjectMapper();
        sp = new Estado();
        sp.setId(1L);
        sp.setSigla("SP");
        sp.setCodigoIbge(ibgeSP);
        rj = new Estado();
        rj.setId(2L);
        rj.setSigla("RJ");
        rj.setCodigoIbge(ibgeRJ);
        lote1 = ResumoLote.build(1L);
        lote1.setUf(ibgeSP);
        lote1.setIdEmissor(58000000L);
        lote1.setDataAbertura(new Date(0));
        lote2 = ResumoLote.build(2L);
        lote2.setUf(ibgeRJ);
        lote2.setIdEmissor(77000000L);
        lote2.setDataAbertura(new Date(0));

        when(cacheLoteRepository.obterLotesAbertos()).thenReturn(Sets.newTreeSet(1, 2));
        when(cacheLoteRepository.obterLotesFechados()).thenReturn(Sets.newTreeSet(3, 4, 5));
        when(cacheLoteRepository.obterLotesEnviados()).thenReturn(Sets.newTreeSet(6, 7, 8, 9));
        when(cacheLoteRepository.obterLotesFinalizados()).thenReturn(Sets.newTreeSet(10, 11, 12, 13, 14));
        when(cacheLoteRepository.obterLotesCancelados()).thenReturn(Sets.newTreeSet(15));

        LongStream.range(1L, 16L).forEach(idLote -> { //start inclusive, end exclusive
            when(cacheLoteRepository.consultarLote(idLote)).thenReturn(idLote % 3 == 2 ? lote2 : lote1);
        });

        when(estadoRepository.findByCodigoIbge(ibgeSP)).thenReturn(sp);
        when(estadoRepository.findByCodigoIbge(ibgeRJ)).thenReturn(rj);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testLotesPorSituacao() throws Exception {
        when(parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, "LOTE_INTERVALO_MONITORACAO_OK", 60)).thenReturn(120);

        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/lote/ciclo")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        verify(parametrosInfraRepository).getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, "LOTE_INTERVALO_MONITORACAO_OK", 60);
        verify(cacheLoteRepository).obterLotesAbertos();
        verify(cacheLoteRepository).obterLotesFechados();
        verify(cacheLoteRepository).obterLotesEnviados();
        verify(cacheLoteRepository).obterLotesFinalizados();
        verify(cacheLoteRepository).obterLotesCancelados();
//        verifyNoMoreInteractions(cacheLoteRepository, estadoRepository);
        Map<String, Map<String, Integer>> result = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals(5, result.size());
        assertEquals((Integer) 2, result.get("Aberto").get(sFalse));
        assertEquals((Integer) 3, result.get("Fechado").get(sFalse));
        assertEquals((Integer) 4, result.get("Enviado").get(sFalse));
        assertEquals((Integer) 5, result.get("Liquidado").get(sFalse));
        assertEquals((Integer) 1, result.get("Cancelado").get(sFalse));
        assertEquals((Integer) 0, result.get("Aberto").get(sTrue));
        assertEquals((Integer) 0, result.get("Fechado").get(sTrue));
        assertEquals((Integer) 0, result.get("Enviado").get(sTrue));
        assertEquals((Integer) 0, result.get("Liquidado").get(sTrue));
        assertEquals((Integer) 0, result.get("Cancelado").get(sTrue));
    }

    @Test
    public void testLotesPorUFeSituacao() throws Exception {
        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/lote/ciclouf")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        verify(cacheLoteRepository).obterLotesAbertos();
        verify(cacheLoteRepository).obterLotesFechados();
        verify(cacheLoteRepository).obterLotesEnviados();
        verify(cacheLoteRepository).obterLotesFinalizados();
        verify(cacheLoteRepository).obterLotesCancelados();
        for (long i = 1L; i <= 15L; i++) {
            verify(cacheLoteRepository).consultarLote(i);
        }
        verify(estadoRepository, times(10)).findByCodigoIbge(ibgeSP);
        verify(estadoRepository, times(5)).findByCodigoIbge(ibgeRJ);
        verifyNoMoreInteractions(cacheLoteRepository, estadoRepository);
        Map<String, Map<String, Integer>> result = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class);
        assertEquals(2, result.size());
        assertEquals((Integer) 1, result.get("SP").get("Aberto")); //1
        assertEquals((Integer) 2, result.get("SP").get("Fechado")); //3,4
        assertEquals((Integer) 3, result.get("SP").get("Enviado")); //6,7,9
        assertEquals((Integer) 3, result.get("SP").get("Liquidado")); //10,12,13
        assertEquals((Integer) 1, result.get("SP").get("Cancelado")); //15
        assertEquals((Integer) 1, result.get("RJ").get("Aberto")); //2
        assertEquals((Integer) 1, result.get("RJ").get("Fechado")); //5
        assertEquals((Integer) 1, result.get("RJ").get("Enviado")); //8
        assertEquals((Integer) 2, result.get("RJ").get("Liquidado")); //11,14
        assertFalse(result.get("RJ").containsKey("Cancelado")); //-
    }

    @Test
    public void testLotesPorIdEmissorRaizSituacao() throws Exception {
        //call via mockMvc
        MvcResult mvcReturn = mockMvc.perform(get("/lote/cicloemissor")
                .contentType(MediaType.APPLICATION_JSON)
        ) //result
                .andExpect(status().isOk())
                .andReturn();

        verify(cacheLoteRepository).obterLotesAbertos();
        verify(cacheLoteRepository).obterLotesFechados();
        verify(cacheLoteRepository).obterLotesEnviados();
        verify(cacheLoteRepository).obterLotesFinalizados();
        verify(cacheLoteRepository).obterLotesCancelados();
        for (long i = 1L; i <= 15L; i++) {
            verify(cacheLoteRepository).consultarLote(i);
        }
        verifyNoMoreInteractions(cacheLoteRepository, estadoRepository);
        Map<String, Map<String, Integer>> result = om.readValue(mvcReturn.getResponse().getContentAsString(), Map.class); //chaves JSON nao podem ser numeros
        assertEquals(2, result.size());
//        result.entrySet().stream().forEach((entry) -> {
//            System.out.println(entry.getKey() + StringUtils.SPACE + entry.getKey().getClass());
//        });
        assertEquals((Integer) 1, result.get("58").get("Aberto")); //1
        assertEquals((Integer) 2, result.get("58").get("Fechado")); //3,4
        assertEquals((Integer) 3, result.get("58").get("Enviado")); //6,7,9
        assertEquals((Integer) 3, result.get("58").get("Liquidado")); //10,12,13
        assertEquals((Integer) 1, result.get("58").get("Cancelado")); //15
        assertEquals((Integer) 1, result.get("77").get("Aberto")); //2
        assertEquals((Integer) 1, result.get("77").get("Fechado")); //5
        assertEquals((Integer) 1, result.get("77").get("Enviado")); //8
        assertEquals((Integer) 2, result.get("77").get("Liquidado")); //11,14
        assertFalse(result.get("77").containsKey("Cancelado")); //-
    }

}
