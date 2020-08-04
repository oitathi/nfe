package com.b2wdigital.fazemu.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.apache.el.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChaveAcessoNFeTest {

    String cUF, dataAAMM, cnpjCpf, mod, serie, nNF, tpEmis, cNF;

    @Before
    public void setUp() {
        cUF = "33";
        dataAAMM = "1812";
        cnpjCpf = "05886614003666";
        mod = "55";
        serie = "46";
        nNF = "9";
        tpEmis = "1";
        cNF = "74023679";
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testCalculaDigitoFull() throws Exception {
        int digito = ChaveAcessoNFe.calculateDV(this.cUF, this.dataAAMM, this.cnpjCpf, this.mod, this.serie, this.nNF, this.tpEmis, this.cNF);
        assertEquals(8, digito);
        assertNotEquals(5, digito);
    }

    @Test
    public void testCalculaDigitoString() throws Exception {
        int digito = ChaveAcessoNFe.calculateDV("3318120588661400366655046000000009174023679");
        assertEquals(8, digito);
        assertNotEquals(5, digito);
    }

    @Test
    public void testCalculaDigitoStringDigitoAMais() {
        try {
            ChaveAcessoNFe.calculateDV("331812058866140036665504600000000917402367911111");
        } catch (ParseException e) {
            assertEquals("Chave de Acesso deve ter 43 ou 44 posições", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void testCalculaDigitoStringNull() {
        try {
            ChaveAcessoNFe.calculateDV(null);
        } catch (ParseException e) {
            assertEquals("Chave de Acesso não pode ser nula", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void testCalculaDigitoStringBranco() {
        try {
            ChaveAcessoNFe.calculateDV("");
        } catch (ParseException e) {
            assertEquals("Chave de Acesso deve ter 43 ou 44 posições", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void testParseKey() throws Exception {
        String key = ChaveAcessoNFe.parseKey(this.cUF, this.dataAAMM, this.cnpjCpf, this.mod, this.serie, this.nNF, this.tpEmis, this.cNF, false);

        assertEquals(key, "3318120588661400366655046000000009174023679");
    }

    @Test
    public void testParseKeyNull() throws Exception {
        String key = ChaveAcessoNFe.parseKey(null, this.dataAAMM, this.cnpjCpf, this.mod, this.serie, this.nNF, this.tpEmis, this.cNF, false);

        assertNotEquals(key, "3318120588661400366655046000000009174023679");
    }

    @Test
    public void testUnparseKey() throws Exception {
        ChaveAcessoNFe key = ChaveAcessoNFe.unparseKey("3318120588661400366655046000000009174023679");

        assertEquals("33", key.cUF);
        assertEquals("1812", key.dataAAMM);
        assertEquals("05886614003666", key.cnpjCpf);
        assertEquals("55", key.mod);
        assertEquals("046", key.serie);
        assertEquals("000000009", key.nNF);
        assertEquals("1", key.tpEmis);
        assertEquals("74023679", key.cNF);
    }

    @Test
    public void testUnparseKeyDigitoAMais() {
        try {
            ChaveAcessoNFe.unparseKey("331812058866140036665504600000000917402367911111");
        } catch (ParseException e) {
            assertEquals("Chave de Acesso deve ter 43 ou 44 posições", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void testUnparseKeyNull() {
        try {
            ChaveAcessoNFe.unparseKey(null);
        } catch (ParseException e) {
            assertEquals("Chave de Acesso não pode ser nula", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void testUnparseKeyBranco() {
        try {
            ChaveAcessoNFe.unparseKey("");
        } catch (ParseException e) {
            assertEquals("Chave de Acesso deve ter 43 ou 44 posições", e.getMessage());
            return;
        }
        fail();
    }

}
