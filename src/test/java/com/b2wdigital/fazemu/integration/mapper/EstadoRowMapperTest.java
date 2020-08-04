/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.Estado;
import java.sql.ResultSet;
import java.sql.Timestamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author dailton.almeida
 */
public class EstadoRowMapperTest {
    @InjectMocks private EstadoRowMapper instance;
    @Mock ResultSet rs = null;
    private final int row = 2;
    private final Long idEstado = 3L;
    private final String sigla = "_sigla";
    private final String nome = "_nome";
    private final Integer codigoIbge = 5;
    private final String usuario = "_usuario";
    private final Timestamp datahora = new Timestamp(7L);
    
    public EstadoRowMapperTest() {
    }
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testMapRow() throws Exception {
        when(rs.getLong(EstadoRowMapper.ID)).thenReturn(idEstado);
        when(rs.getString(EstadoRowMapper.SIGLA)).thenReturn(sigla);
        when(rs.getString(EstadoRowMapper.NOME)).thenReturn(nome);
        when(rs.getInt(EstadoRowMapper.COD_IBGE)).thenReturn(codigoIbge);
        when(rs.getString(EstadoRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(EstadoRowMapper.DATAHORA)).thenReturn(datahora);

        //call
        Estado result = instance.mapRow(rs, row);

        verify(rs).getLong(EstadoRowMapper.ID);
        verify(rs).getString(EstadoRowMapper.SIGLA);
        verify(rs).getString(EstadoRowMapper.NOME);
        verify(rs).getInt(EstadoRowMapper.COD_IBGE);
        verify(rs).getString(EstadoRowMapper.USUARIO);
        verify(rs).getTimestamp(EstadoRowMapper.DATAHORA);
        verifyNoMoreInteractions(rs);
        assertEquals(idEstado, result.getId());
        assertEquals(sigla, result.getSigla());
        assertEquals(nome, result.getNome());
        assertEquals(codigoIbge, result.getCodigoIbge());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
    }
    
}
