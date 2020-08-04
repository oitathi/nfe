/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.enumeration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dailton.almeida
 */
public class SituacaoLoteEnumTest {
    
    public SituacaoLoteEnumTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testCodigos() {
        String[] expResult = new String[] {"A", "F", "E", "V", "L", "C"};
        String[] result = SituacaoLoteEnum.codigos();
        assertArrayEquals(expResult, result);
    }

    @Test
    public void testDescricoes() {
        String[] expResult = new String[] {"Aberto", "Fechado", "Erro", "Enviado", "Liquidado", "Cancelado"};
        String[] result = SituacaoLoteEnum.descricoes();
        assertArrayEquals(expResult, result);
    }
    
}
