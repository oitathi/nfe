/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.dao.cached;

import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.domain.Estado;
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
public class EstadoCachedDaoTest {
    @Mock private EstadoRepository delegateRepository;
    private final Long id = 2L;
    private final Long lid = id.longValue();
    private final Integer codigoIbge = 41;
    private Estado estado;
    
    public EstadoCachedDaoTest() {
    }
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        estado = new Estado();
        estado.setId(lid);
        estado.setCodigoIbge(codigoIbge);
        when(delegateRepository.listAll()).thenReturn(Arrays.asList(estado));
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testFindByIdEntryNotInCache() {
        EstadoCachedDao instance = new EstadoCachedDao(delegateRepository);
        
        Estado result = instance.findById(id);

        verify(delegateRepository).listAll(); //valida que o repositorio delegado foi usado para obter a informacao
        assertEquals(lid, result.getId());
    }
    @Test
    public void testFindByIdEntryInCacheTwoGetsNoExpiration() throws Exception {
        EstadoCachedDao instance = new EstadoCachedDao(delegateRepository);
        
        Estado result1 = instance.findById(id);
        Estado result2 = instance.findById(id);

        verify(delegateRepository).listAll(); //valida que o repositorio delegado foi usado apenas uma vez para obter a informacao
        assertEquals(lid, result1.getId());
        assertEquals(lid, result2.getId());
    }
    @Test
    public void testFindByIdEntryInCacheThreeGetsOneExpiration() throws Exception {
        int cacheTime = 500, sleepTime = 300;
        EstadoCachedDao instance = new EstadoCachedDao(delegateRepository, cacheTime, TimeUnit.MILLISECONDS);
        
        Estado result1 = instance.findById(id);
        Thread.sleep(sleepTime); //tempo do cache eh de 500 milisegundos
        Estado result2 = instance.findById(id);
        Thread.sleep(sleepTime); //tempo do cache eh de 500 milisegundos; terceira chamada abiaxo deve acionar o repositorio delegado novamente
        Estado result3 = instance.findById(id);

        verify(delegateRepository, times(2)).listAll(); //valida que o repositorio delegado foi usado duas vezes para obter a informacao
        assertEquals(lid, result1.getId());
        assertEquals(lid, result2.getId());
        assertEquals(lid, result3.getId());
    }

    @Test
    public void testFindByCodigoIbgeEntryNotInCache() {
        EstadoCachedDao instance = new EstadoCachedDao(delegateRepository);
        
        Estado result = instance.findByCodigoIbge(codigoIbge);

        verify(delegateRepository).listAll(); //valida que o repositorio delegado foi usado para obter a informacao
        assertEquals(lid, result.getId());
    }
    @Test
    public void testFindByCodigoIbgeEntryInCacheTwoGetsNoExpiration() throws Exception {
        EstadoCachedDao instance = new EstadoCachedDao(delegateRepository);
        
        Estado result1 = instance.findByCodigoIbge(codigoIbge);
        Estado result2 = instance.findByCodigoIbge(codigoIbge);

        verify(delegateRepository).listAll(); //valida que o repositorio delegado foi usado apenas uma vez para obter a informacao
        assertEquals(lid, result1.getId());
        assertEquals(lid, result2.getId());
    }
    @Test
    public void testFindByCodigoIbgeEntryInCacheThreeGetsOneExpiration() throws Exception {
        int cacheTime = 500, sleepTime = 300;
        EstadoCachedDao instance = new EstadoCachedDao(delegateRepository, cacheTime, TimeUnit.MILLISECONDS);
        
        Estado result1 = instance.findByCodigoIbge(codigoIbge);
        Thread.sleep(sleepTime); //tempo do cache eh de 500 milisegundos
        Estado result2 = instance.findByCodigoIbge(codigoIbge);
        Thread.sleep(sleepTime); //tempo do cache eh de 500 milisegundos; terceira chamada abiaxo deve acionar o repositorio delegado novamente
        Estado result3 = instance.findByCodigoIbge(codigoIbge);

        verify(delegateRepository, times(2)).listAll(); //valida que o repositorio delegado foi usado duas vezes para obter a informacao
        assertEquals(lid, result1.getId());
        assertEquals(lid, result2.getId());
        assertEquals(lid, result3.getId());
    }
    
}
