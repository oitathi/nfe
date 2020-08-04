/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.dao.cached;

import com.b2wdigital.fazemu.business.repository.TipoEmissaoRepository;
import com.b2wdigital.fazemu.domain.TipoEmissao;
import java.util.Arrays;
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
public class TipoEmissaoCachedDaoTest {
    @Mock private TipoEmissaoRepository delegateRepository;
    private final Long id = 2L;
    private TipoEmissao tipoEmissao;
    
    public TipoEmissaoCachedDaoTest() {
    }
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tipoEmissao = new TipoEmissao();
        tipoEmissao.setId(id);
        when(delegateRepository.listAtivos()).thenReturn(Arrays.asList(tipoEmissao));
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testFindByIdEntryNotInCache() {
        TipoEmissaoCachedDao instance = new TipoEmissaoCachedDao(delegateRepository);
        
        TipoEmissao result = instance.findById(id);

        verify(delegateRepository).listAtivos(); //valida que o repositorio delegado foi usado para obter a informacao
        assertEquals(id, result.getId());
    }
    @Test
    public void testFindByIdEntryInCacheTwoGetsNoExpiration() throws Exception {
        TipoEmissaoCachedDao instance = new TipoEmissaoCachedDao(delegateRepository);
        
        TipoEmissao result1 = instance.findById(id);
        TipoEmissao result2 = instance.findById(id);

        verify(delegateRepository).listAtivos(); //valida que o repositorio delegado foi usado apenas uma vez para obter a informacao
        assertEquals(id, result1.getId());
        assertEquals(id, result2.getId());
    }
    @Test
    public void testFindByIdEntryInCacheThreeGetsOneExpiration() throws Exception {
        int cacheTime = 500, sleepTime = 300;
        TipoEmissaoCachedDao instance = new TipoEmissaoCachedDao(delegateRepository, cacheTime, TimeUnit.MILLISECONDS);
        
        TipoEmissao result1 = instance.findById(id);
        Thread.sleep(sleepTime); //tempo do cache eh de 500 milisegundos
        TipoEmissao result2 = instance.findById(id);
        Thread.sleep(sleepTime); //tempo do cache eh de 500 milisegundos; terceira chamada abiaxo deve acionar o repositorio delegado novamente
        TipoEmissao result3 = instance.findById(id);

        verify(delegateRepository, times(2)).listAtivos(); //valida que o repositorio delegado foi usado duas vezes para obter a informacao
        assertEquals(id, result1.getId());
        assertEquals(id, result2.getId());
        assertEquals(id, result3.getId());
    }
    
}
