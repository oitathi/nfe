package com.b2wdigital.fazemu.service.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.b2wdigital.fazemu.business.repository.CacheLoteRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.FecharEnviarLoteService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.scheduled.disabled.FecharLotesAbertosScheduled;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.google.common.collect.Sets;

/**
 *
 * @author dailton.almeida
 */
public class FecharLotesAbertosServiceImplTest {

    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @InjectMocks
    private FecharLotesAbertosScheduled instance;
    @Mock
    private FecharEnviarLoteService fecharEnviarLoteService;
    @Mock
    private CacheLoteRepository cacheLoteRepository;
    @Mock
    private ParametrosInfraRepository parametrosInfraRepository;
    @Mock
    private RedisOperationsService redisOperationsService;

    public FecharLotesAbertosServiceImplTest() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFecharLotesAbertosRegraDoTempo() {
        long now = System.currentTimeMillis();
        long idLote1 = 1, idLote2 = 2, idLote3 = 3;
        ResumoLote lote1 = ResumoLote.build(idLote1);
        lote1.setDataAbertura(new Date(now - 120 * 1000));
        ResumoLote lote2 = ResumoLote.build(idLote2);
        lote2.setDataAbertura(new Date(now));
        ResumoLote lote3 = ResumoLote.build(idLote3);
        lote3.setDataAbertura(new Date(now - 180 * 1000));
        Set<Object> set = Sets.newHashSet((int) idLote1, (int) idLote2, (int) idLote3);
        String KEY = "semaforo01";

        when(parametrosInfraRepository.getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_ESPERA_FECHAMENTO_LOTE, 15)).thenReturn(20);
        when(cacheLoteRepository.consultarLote(idLote1)).thenReturn(lote1);
        when(cacheLoteRepository.consultarLote(idLote2)).thenReturn(lote2);
        when(cacheLoteRepository.consultarLote(idLote3)).thenReturn(lote3);
        when(redisOperationsService.difference("lotesAbertos", KEY)).thenReturn(set);

        //call
        instance.fecharLotesAbertos();

        verify(parametrosInfraRepository).getAsInteger(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_TEMPO_ESPERA_FECHAMENTO_LOTE, 15);
        verify(cacheLoteRepository).consultarLote(idLote1);
        verify(cacheLoteRepository).consultarLote(idLote2);
        verify(cacheLoteRepository).consultarLote(idLote3);
        verify(fecharEnviarLoteService).fecharEnviarLote(idLote1);
        //idLote2 nao deve ser fechado
        verify(fecharEnviarLoteService).fecharEnviarLote(idLote3);
        verify(redisOperationsService).difference("lotesAbertos", KEY);
    }

}
