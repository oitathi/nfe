/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.dao.jdbc;

import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.integration.dao.jdbc.EstadoJdbcDao;
import com.b2wdigital.fazemu.integration.mapper.EstadoRowMapper;
import java.util.List;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 *
 * @author dailton.almeida
 */
public class EstadoJdbcDaoTest {
    private EmbeddedDatabase db;
    private EstadoJdbcDao instance;
    
    public EstadoJdbcDaoTest() {
    }
    
    @Before
    public void setUp() {
        db = new EmbeddedDatabaseBuilder()
                .addScript("db/estadodao_createtable.sql")
                .addScript("db/estadodao_insertdata.sql")
                .build();
        
        instance = new EstadoJdbcDao();
        ReflectionTestUtils.setField(instance, "namedParameterJdbcOperations", new NamedParameterJdbcTemplate(db));
        ReflectionTestUtils.setField(instance, "estadoRowMapper", new EstadoRowMapper());
    }
    
    @After
    public void tearDown() {
        db.shutdown();
    }

    @Test
    public void testFindAll() {
        //call
        List<Estado> result = instance.listAll();

        assertEquals("Minas Gerais"  , result.get(0).getNome());
        assertEquals("Rio de Janeiro", result.get(1).getNome());
        assertEquals("Sao Paulo"     , result.get(2).getNome());
    }
    
    @Test
    public void testFindById() {
        Long id = NumberUtils.LONG_ONE;

        //call
        Estado result = instance.findById(id);

        assertEquals("Sao Paulo", result.getNome());
    }

    @Test
    public void testFindByCodigoIbge() {
        Integer codigoIbge = 33;

        //call
        Estado result = instance.findByCodigoIbge(codigoIbge);

        assertEquals("Rio de Janeiro", result.getNome());
    }
    
}
