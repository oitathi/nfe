/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.Usuario;
import java.sql.ResultSet;
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
public class UsuarioRowMapperTest {
    @InjectMocks private UsuarioRowMapper instance;
    @Mock ResultSet rs = null;
    private final int row = 2;
    private final String idUsuario = "_idusuario";
    private final String nome = "_nome";
    
    public UsuarioRowMapperTest() {
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
        when(rs.getString(UsuarioRowMapper.ID)).thenReturn(idUsuario);
        when(rs.getString(UsuarioRowMapper.NOME)).thenReturn(nome);

        //call
        Usuario result = instance.mapRow(rs, row);

        verify(rs).getString(UsuarioRowMapper.ID);
        verify(rs).getString(UsuarioRowMapper.NOME);
        verifyNoMoreInteractions(rs);
        assertEquals(idUsuario, result.getId());
        assertEquals(nome, result.getNome());
    }
    
}
