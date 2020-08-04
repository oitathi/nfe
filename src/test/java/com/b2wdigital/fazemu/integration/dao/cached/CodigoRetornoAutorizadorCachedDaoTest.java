/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.dao.cached;

import com.b2wdigital.fazemu.business.repository.CodigoRetornoAutorizadorRepository;
import com.b2wdigital.fazemu.domain.CodigoRetornoAutorizador;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author dailton.almeida
 */
public class CodigoRetornoAutorizadorCachedDaoTest {
    @Mock private CodigoRetornoAutorizadorRepository delegateRepository;
    private final Integer cStat = 41;
    private final String strValue = "_value";
    private CodigoRetornoAutorizador crau;
    
    public CodigoRetornoAutorizadorCachedDaoTest() {
    }
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        crau = CodigoRetornoAutorizador.build(cStat);
        crau.setSituacaoAutorizador(strValue);
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testGetSituacaoAutorizacaooByIdEntryNotInCache() {
        CodigoRetornoAutorizadorCachedDao instance = new CodigoRetornoAutorizadorCachedDao(delegateRepository);
        
        when(delegateRepository.findById(cStat)).thenReturn(crau);
        
        String result = instance.getSituacaoAutorizacaoById(cStat);

        verify(delegateRepository).findById(cStat); //valida que o repositorio delegado foi usado para obter a informacao
        assertSame(strValue, result);
    }
    @Test
    public void testGetSituacaoAutorizacaooByIdEntryInCacheTwoGetsNoExpiration() throws Exception {
        CodigoRetornoAutorizadorCachedDao instance = new CodigoRetornoAutorizadorCachedDao(delegateRepository);
        
        when(delegateRepository.findById(cStat)).thenReturn(crau);
        
        String result1 = instance.getSituacaoAutorizacaoById(cStat);
        String result2 = instance.getSituacaoAutorizacaoById(cStat);

        verify(delegateRepository).findById(cStat); //valida que o repositorio delegado foi usado apenas uma vez para obter a informacao
        assertSame(strValue, result1);
        assertSame(strValue, result2);
    }
    @Test
    public void testGetSituacaoAutorizacaooByIdEntryInCacheThreeGetsOneExpiration() throws Exception {
        int cacheTime = 500, sleepTime = 300;
        CodigoRetornoAutorizadorCachedDao instance = new CodigoRetornoAutorizadorCachedDao(delegateRepository, cacheTime, TimeUnit.MILLISECONDS);
        
        when(delegateRepository.findById(cStat)).thenReturn(crau);
        
        String result1 = instance.getSituacaoAutorizacaoById(cStat);
        Thread.sleep(sleepTime); //tempo do cache eh de 500 milisegundos
        String result2 = instance.getSituacaoAutorizacaoById(cStat);
        Thread.sleep(sleepTime); //tempo do cache eh de 500 milisegundos; terceira chamada abiaxo deve acionar o repositorio delegado novamente
        String result3 = instance.getSituacaoAutorizacaoById(cStat);

        verify(delegateRepository, times(2)).findById(cStat); //valida que o repositorio delegado foi usado duas vezes para obter a informacao
        assertSame(strValue, result1);
        assertSame(strValue, result2);
        assertSame(strValue, result3);
    }
    
}
