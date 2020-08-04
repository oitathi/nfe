/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.dao.jdbc;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.integration.dao.jdbc.DocumentoEventoJdbcDao;
import com.b2wdigital.fazemu.integration.mapper.DocumentoEventoRowMapper;

/**
 *
 * @author dailton.almeida
 */
public class DocumentoEventoJdbcDaoTest {
    private EmbeddedDatabase db;
    private DocumentoEventoJdbcDao instance;
//    private final Long idEvento = 1L;
    private final Long idDocFiscal = 2L;
//    private final Long idXml = 40L;
//    private final String usuario = "U";
//    private final Date dataHora = new Date(17L);
    
    public DocumentoEventoJdbcDaoTest() {
    }
    
    @Before
    public void setUp() {
        db = new EmbeddedDatabaseBuilder()
                .addScript("db/documentoeventodao_createtable.sql")
                .addScript("db/documentoeventodao_insertdata.sql")
                .build();
        
        instance = new DocumentoEventoJdbcDao();
        ReflectionTestUtils.setField(instance, "namedParameterJdbcOperations", new NamedParameterJdbcTemplate(db));
        ReflectionTestUtils.setField(instance, "documentoEventoRowMapper", new DocumentoEventoRowMapper());
    }
    
    @After
    public void tearDown() {
        db.shutdown();
    }

    @Test
    public void testFindByIdDocFiscal() {
        //call
        List<DocumentoEvento> result = instance.listByIdDocFiscal(idDocFiscal);
        
        assertEquals(2, result.size());
        assertEquals("DRAW", result.get(0).getIdPonto());
        assertEquals((Long)20L, result.get(0).getIdXml());
        assertEquals("DRAW", result.get(1).getIdPonto());
        assertEquals((Long)30L, result.get(1).getIdXml());
    }
    
}
