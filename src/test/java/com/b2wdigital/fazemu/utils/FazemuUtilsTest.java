package com.b2wdigital.fazemu.utils;

import java.util.Calendar;
import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dailton.almeida
 */
public class FazemuUtilsTest {
    
    public FazemuUtilsTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testObterRaizCNPJ1() {
        Long idEmissor = 12345678000102L;
        Long expResult = 12345678L;

        Long result = FazemuUtils.obterRaizCNPJ(idEmissor);

        assertEquals(expResult, result);
    }
    @Test
    public void testObterRaizCNPJ2() {
        Long idEmissor = 345678000102L;
        Long expResult = 345678L;

        Long result = FazemuUtils.obterRaizCNPJ(idEmissor);

        assertEquals(expResult, result);
    }
    
    @Test
    public void testDeltaInSeconds() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2020, Calendar.JANUARY, 1, 22, 57, 43);
        Date d2 = cal.getTime();
        cal.clear();
        cal.set(2020, Calendar.JANUARY, 1, 22, 57, 39);
        Date d1 = cal.getTime();

        long result = FazemuUtils.deltaInSeconds(d2, d1);

        assertEquals(4L, result);
    }

    @Test
    public void testMaskNFeNumberShorter() {
        assertEquals("000.001.234", FazemuUtils.maskNFeNumber("1234"));
    }
    @Test
    public void testMaskNFeNumberFull() {
        assertEquals("123.456.789", FazemuUtils.maskNFeNumber("123456789"));
    }

    @Test
    public void testMaskCPFShorter() {
        assertEquals("000.000.001-91", FazemuUtils.maskCPF("191"));
    }
    @Test
    public void testMaskCPFFull() {
        assertEquals("123.456.789-01", FazemuUtils.maskCPF("12345678901"));
    }

    @Test
    public void testMaskCNPJShorter() {
        assertEquals("00.000.001/2345-67", FazemuUtils.maskCNPJ("1234567"));
    }
    @Test
    public void testMaskCNPJFull() {
        assertEquals("12.345.678/9012-34", FazemuUtils.maskCNPJ("12345678901234"));
    }

    @Test
    public void testMaskCNPJorCPF_CNPJ() {
        assertEquals("00.000.001/2345-67", FazemuUtils.maskCNPJorCPF("1234567", null));
    }
    @Test
    public void testMaskCNPJorCPF_CPF() {
        assertEquals("000.000.001-91", FazemuUtils.maskCNPJorCPF(null, "191"));
    }
    
    @Test
    public void testNormalizarChaveAcessoSemPrefixo()  {
        assertEquals("abc", FazemuUtils.normalizarChaveAcesso("abc"));
    }
    @Test
    public void testNormalizarChaveAcessoComPrefixo()  {
        assertEquals("abc", FazemuUtils.normalizarChaveAcesso("NFeabc"));
    }
    @Test
    public void testNormalizarChaveAcessoComPrefixoCaixaAlta()  {
        assertEquals("abc", FazemuUtils.normalizarChaveAcesso("NFEabc"));
    }
}
