/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.dao.cached;

import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author dailton.almeida
 */
public class ParametrosInfraCachedDaoTest {

    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    @Mock
    private ParametrosInfraRepository delegateRepository;
    private final String tipoDocumentoFiscal = TIPO_DOCUMENTO_FISCAL_NFE;
    private final String key = "_key";
    private final String strValue = "_value";
    private final String strDefaultValue = "_defaultvalue";
    private final String strIntValue = "41";
    private final Integer intValue = 41;
    private final Integer intDefaultValue = intValue + 1; //precisa ser diferente do intValue

    public ParametrosInfraCachedDaoTest() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetAsStringEntryNotInCache() {
        ParametrosInfraCachedDao instance = new ParametrosInfraCachedDao(tipoDocumentoFiscal, delegateRepository);

        when(delegateRepository.getAsString(tipoDocumentoFiscal, key)).thenReturn(strValue);

        String result = instance.getAsString(tipoDocumentoFiscal, key);

        verify(delegateRepository).getAsString(tipoDocumentoFiscal, key); //valida que o repositorio delegado foi usado para obter a informacao
        assertSame(strValue, result);
    }

    @Test
    public void testGetAsStringEntryInCacheTwoGetsNoExpiration() throws Exception {
        ParametrosInfraCachedDao instance = new ParametrosInfraCachedDao(tipoDocumentoFiscal, delegateRepository);

        when(delegateRepository.getAsString(tipoDocumentoFiscal, key)).thenReturn(strValue);

        String result1 = instance.getAsString(tipoDocumentoFiscal, key);
        String result2 = instance.getAsString(tipoDocumentoFiscal, key);

        verify(delegateRepository).getAsString(tipoDocumentoFiscal, key); //valida que o repositorio delegado foi usado apenas uma vez para obter a informacao
        assertSame(strValue, result1);
        assertSame(strValue, result2);
    }

    @Test
    public void testGetAsStringEntryInCacheThreeGetsOneExpiration() throws Exception {
        ParametrosInfraCachedDao instance = new ParametrosInfraCachedDao(tipoDocumentoFiscal, delegateRepository, 500, TimeUnit.MILLISECONDS);

        when(delegateRepository.getAsString(tipoDocumentoFiscal, key)).thenReturn(strValue);

        String result1 = instance.getAsString(tipoDocumentoFiscal, key);
        Thread.sleep(300); //tempo do cache eh de 500 milisegundos
        String result2 = instance.getAsString(tipoDocumentoFiscal, key);
        Thread.sleep(300); //tempo do cache eh de 500 milisegundos; terceira chamada abiaxo deve acionar o repositorio delegado novamente
        String result3 = instance.getAsString(tipoDocumentoFiscal, key);

        verify(delegateRepository, times(2)).getAsString(tipoDocumentoFiscal, key); //valida que o repositorio delegado foi usado duas vezes para obter a informacao
        assertSame(strValue, result1);
        assertSame(strValue, result2);
        assertSame(strValue, result3);
    }

    @Test
    public void testGetAsStringWithDefaultEntryNotInCache() {
        ParametrosInfraCachedDao instance = new ParametrosInfraCachedDao(tipoDocumentoFiscal, delegateRepository);

        when(delegateRepository.getAsString(tipoDocumentoFiscal, key)).thenReturn(StringUtils.EMPTY);

        String result = instance.getAsString(tipoDocumentoFiscal, key, strDefaultValue);

        verify(delegateRepository).getAsString(tipoDocumentoFiscal, key); //valida que o repositorio delegado foi usado para obter a informacao
        assertSame(strDefaultValue, result);
    }

    @Test
    public void testGetAsStringWithDefaultEntryInCacheTwoGetsNoExpiration() throws Exception {
        ParametrosInfraCachedDao instance = new ParametrosInfraCachedDao(tipoDocumentoFiscal, delegateRepository);

        when(delegateRepository.getAsString(tipoDocumentoFiscal, key)).thenReturn(StringUtils.EMPTY);

        String result1 = instance.getAsString(tipoDocumentoFiscal, key, strDefaultValue);
        String result2 = instance.getAsString(tipoDocumentoFiscal, key, strDefaultValue);

        verify(delegateRepository).getAsString(tipoDocumentoFiscal, key); //valida que o repositorio delegado foi usado apenas uma vez para obter a informacao
        assertSame(strDefaultValue, result1);
        assertSame(strDefaultValue, result2);
    }

    @Test
    public void testGetAsStringWithDefaultEntryInCacheThreeGetsOneExpiration() throws Exception {
        ParametrosInfraCachedDao instance = new ParametrosInfraCachedDao(tipoDocumentoFiscal, delegateRepository, 500, TimeUnit.MILLISECONDS);

        when(delegateRepository.getAsString(tipoDocumentoFiscal, key)).thenReturn(StringUtils.EMPTY);

        String result1 = instance.getAsString(tipoDocumentoFiscal, key, strDefaultValue);
        Thread.sleep(300); //tempo do cache eh de 500 milisegundos
        String result2 = instance.getAsString(tipoDocumentoFiscal, key, strDefaultValue);
        Thread.sleep(300); //tempo do cache eh de 500 milisegundos; terceira chamada abiaxo deve acionar o repositorio delegado novamente
        String result3 = instance.getAsString(tipoDocumentoFiscal, key, strDefaultValue);

        verify(delegateRepository, times(2)).getAsString(tipoDocumentoFiscal, key); //valida que o repositorio delegado foi usado duas vezes para obter a informacao
        assertSame(strDefaultValue, result1);
        assertSame(strDefaultValue, result2);
        assertSame(strDefaultValue, result3);
    }

    @Test
    public void testGetAsIntegerEntryNotInCache() {
        ParametrosInfraCachedDao instance = new ParametrosInfraCachedDao(tipoDocumentoFiscal, delegateRepository);

        when(delegateRepository.getAsString(tipoDocumentoFiscal, key)).thenReturn(strIntValue);

        Integer result = instance.getAsInteger(tipoDocumentoFiscal, key);

        verify(delegateRepository).getAsString(tipoDocumentoFiscal, key); //valida que o repositorio delegado foi usado para obter a informacao
        assertEquals(intValue, result);
    }

    @Test
    public void testGetAsIntegerEntryInCacheTwoGetsNoExpiration() throws Exception {
        ParametrosInfraCachedDao instance = new ParametrosInfraCachedDao(tipoDocumentoFiscal, delegateRepository);

        when(delegateRepository.getAsString(tipoDocumentoFiscal, key)).thenReturn(strIntValue);

        Integer result1 = instance.getAsInteger(tipoDocumentoFiscal, key);
        Integer result2 = instance.getAsInteger(tipoDocumentoFiscal, key);

        verify(delegateRepository).getAsString(tipoDocumentoFiscal, key); //valida que o repositorio delegado foi usado apenas uma vez para obter a informacao
        assertEquals(intValue, result1);
        assertEquals(intValue, result2);
    }

    @Test
    public void testGetAsIntegerEntryInCacheThreeGetsOneExpiration() throws Exception {
        ParametrosInfraCachedDao instance = new ParametrosInfraCachedDao(tipoDocumentoFiscal, delegateRepository, 500, TimeUnit.MILLISECONDS);

        when(delegateRepository.getAsString(tipoDocumentoFiscal, key)).thenReturn(strIntValue);

        Integer result1 = instance.getAsInteger(tipoDocumentoFiscal, key);
        Thread.sleep(300); //tempo do cache eh de 500 milisegundos
        Integer result2 = instance.getAsInteger(tipoDocumentoFiscal, key);
        Thread.sleep(300); //tempo do cache eh de 500 milisegundos; terceira chamada abiaxo deve acionar o repositorio delegado novamente
        Integer result3 = instance.getAsInteger(tipoDocumentoFiscal, key);

        verify(delegateRepository, times(2)).getAsString(tipoDocumentoFiscal, key); //valida que o repositorio delegado foi usado duas vezes para obter a informacao
        assertEquals(intValue, result1);
        assertEquals(intValue, result2);
        assertEquals(intValue, result3);
    }

    @Test
    public void testGetAsIntegerWithDefaultEntryNotInCache() {
        ParametrosInfraCachedDao instance = new ParametrosInfraCachedDao(tipoDocumentoFiscal, delegateRepository);

        when(delegateRepository.getAsString(tipoDocumentoFiscal, key)).thenReturn(StringUtils.EMPTY);

        Integer result = instance.getAsInteger(tipoDocumentoFiscal, key, intDefaultValue);

        verify(delegateRepository).getAsString(tipoDocumentoFiscal, key); //valida que o repositorio delegado foi usado para obter a informacao
        assertEquals(intDefaultValue, result);
    }

    @Test
    public void testGetAsIntegerWithDefaultEntryInCacheTwoGetsNoExpiration() throws Exception {
        ParametrosInfraCachedDao instance = new ParametrosInfraCachedDao(tipoDocumentoFiscal, delegateRepository);

        when(delegateRepository.getAsString(tipoDocumentoFiscal, key)).thenReturn(StringUtils.EMPTY);

        Integer result1 = instance.getAsInteger(tipoDocumentoFiscal, key, intDefaultValue);
        Integer result2 = instance.getAsInteger(tipoDocumentoFiscal, key, intDefaultValue);

        verify(delegateRepository).getAsString(tipoDocumentoFiscal, key); //valida que o repositorio delegado foi usado apenas uma vez para obter a informacao
        assertEquals(intDefaultValue, result1);
        assertEquals(intDefaultValue, result2);
    }

    @Test
    public void testGetAsIntegerWithDefaultEntryInCacheThreeGetsOneExpiration() throws Exception {
        ParametrosInfraCachedDao instance = new ParametrosInfraCachedDao(tipoDocumentoFiscal, delegateRepository, 500, TimeUnit.MILLISECONDS);

        when(delegateRepository.getAsString(tipoDocumentoFiscal, key)).thenReturn(StringUtils.EMPTY);

        Integer result1 = instance.getAsInteger(tipoDocumentoFiscal, key, intDefaultValue);
        Thread.sleep(300); //tempo do cache eh de 500 milisegundos
        Integer result2 = instance.getAsInteger(tipoDocumentoFiscal, key, intDefaultValue);
        Thread.sleep(300); //tempo do cache eh de 500 milisegundos; terceira chamada abiaxo deve acionar o repositorio delegado novamente
        Integer result3 = instance.getAsInteger(tipoDocumentoFiscal, key, intDefaultValue);

        verify(delegateRepository, times(2)).getAsString(tipoDocumentoFiscal, key); //valida que o repositorio delegado foi usado duas vezes para obter a informacao
        assertEquals(intDefaultValue, result1);
        assertEquals(intDefaultValue, result2);
        assertEquals(intDefaultValue, result3);
    }
}
