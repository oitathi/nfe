package com.b2wdigital.fazemu.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

import javax.xml.bind.JAXBContext;

import org.apache.commons.io.IOUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.ConsultarReciboService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe.InfProt;
import com.google.common.collect.Sets;

/**
 *
 * @author dailton.almeida
 */
public class ConsultarReciboServiceImplTest {

    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @InjectMocks
    private ConsultarReciboService consultaReciboService;

    @Mock
    private CacheLoteRepository cacheLoteRepository;

    @Mock
    private ParametrosInfraRepository parametrosInfraRepository;

    @Mock
    private RedisOperationsService redisOperationsService;

    @Mock
    private final Long idDocFiscal = 2L;
    private final String usuario = "_usuario";

    public ConsultarReciboServiceImplTest() {
    }

    //@Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(consultaReciboService, "context", JAXBContext.newInstance(TNFe.class, TNfeProc.class));
    }

    //@After
    public void tearDown() {
    }

    protected String load(String resourceName) throws Exception {
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourceName), StandardCharsets.UTF_8);
    }

    //@Test
    public void testEncaixarProtocolo() throws Exception {
        String expResult = load("nfeProc_55_46_120.xml");
        String xmlNFe = load("NFe_55_46_120.xml");
        InfProt infProt = new InfProt();
        infProt.setCStat("123");
        TProtNFe protNFe = new TProtNFe();
        protNFe.setVersao("4.00");
        protNFe.setInfProt(infProt);

        DocumentoFiscal docu = new DocumentoFiscal();
        docu.setId(idDocFiscal);

        //call
        String result = consultaReciboService.encaixarProtocolo(xmlNFe, protNFe, docu, usuario);

        assertEquals(expResult, result);
    }

    //@Test
    public void testConsultarRecibosRegraDoTempo() {
        long now = System.currentTimeMillis();
        long idLote1 = 2, idLote2 = 3, idLote3 = 5;
        ResumoLote lote1 = ResumoLote.build(idLote1);
        lote1.setDataUltimaConsultaRecibo(null);
        ResumoLote lote2 = ResumoLote.build(idLote2);
        lote2.setDataUltimaConsultaRecibo(new Date(now));
        ResumoLote lote3 = ResumoLote.build(idLote3);
        lote3.setDataUltimaConsultaRecibo(new Date(now - 180 * 1000));
        Set<Object> set = Sets.newHashSet((int) idLote1, (int) idLote2, (int) idLote3);
        String KEY = "semaforo02";

        when(parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, "SEFAZ_INTERVALO_MIN_CONSULTA_RECIBO_LOTE", 120)).thenReturn(120);
        when(cacheLoteRepository.consultarLote(idLote1)).thenReturn(lote1);
        when(cacheLoteRepository.consultarLote(idLote2)).thenReturn(lote2);
        when(cacheLoteRepository.consultarLote(idLote3)).thenReturn(lote3);
        when(redisOperationsService.difference("lotesEnviados", KEY)).thenReturn(set);

        ConsultarReciboService service = spy(consultaReciboService);
//        doNothing().when(service).consultarRecibo(any(ResumoLote.class));

        //call
//        service.consultarRecibos();
        verify(parametrosInfraRepository).getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, "SEFAZ_INTERVALO_MIN_CONSULTA_RECIBO_LOTE", 120);
        verify(cacheLoteRepository).consultarLote(idLote1);
        verify(cacheLoteRepository).consultarLote(idLote2);
        verify(cacheLoteRepository).consultarLote(idLote3);
        verify(service).consultarRecibo(lote1);
        //lote2 nao deve ser consultado
        verify(service).consultarRecibo(lote3);
    }

}
