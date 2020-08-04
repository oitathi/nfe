/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.domain;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dailton.almeida
 */

public class ResumoLoteTest {
    private ResumoLote instance;
    private final Long idLote = 2L;
    
    public ResumoLoteTest() {
    } 
    
    @Before
    public void setUp() {
        instance = ResumoLote.build(idLote);
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testEqualsNullInput() {
        Object o = null;
        assertNotEquals(o, instance);
    }
    @Test
    public void testEqualsSameInput() {
        assertEquals(instance, instance);
    }
    @Test
    public void testEqualsOtherClassInput() {
        Object o = Boolean.TRUE;
        assertNotEquals(o, instance);
    }
    @Test
    public void testEqualsSameClassDifferent() {
        ResumoLote other = ResumoLote.build(idLote + 1L);
        assertNotEquals(other, instance);
    }
    @Test
    public void testEqualsSameClassEquals() {
        ResumoLote other = ResumoLote.build(idLote);
        assertNotSame(other, instance);
        assertEquals(other, instance);
    }

    @Test
    public void testToString() {
        instance.setTipoDocumentoFiscal("NFE");
        instance.setIdEmissor(11L);
        instance.setUf(3);
        instance.setMunicipio(1L);
        instance.setTipoEmissao(5);
        instance.setVersao("_versao");
        instance.setQuantidade(7);
        instance.setTamanho(500);
        instance.setRecibo("_recibo");
        instance.setServico("_servico");
        String expResult = "ResumoLote[idLote=2,tipoDocumentoFiscal=NFE,idEmissor=11,uf=3,municipio=1,tipoEmissao=5,versao=_versao,idDocFiscalList=<null>,quantidade=7,tamanho=500,dataAbertura=<null>,dataUltimaAlteracao=<null>,recibo=_recibo,dataUltimaConsultaRecibo=<null>,servico=_servico]";
        String result = instance.toString();
        assertEquals(expResult, result);
    }
    
}
