/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.EmissorRaiz;
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
public class EmissorRaizRowMapperTest {
    @InjectMocks private EmissorRaizRowMapper instance;
    @Mock private ResultSet rs;
    private final Integer row = 2;
    private final Long idEmissorRaiz = 3L, idImpressora = 4L;
    private final String nome = "_nome", situacao = "_sit", usuario = "_usuario";
    private final Timestamp datahora = new Timestamp(5L);
    
    public EmissorRaizRowMapperTest() {
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
        when(rs.getLong(EmissorRaizRowMapper.ID)).thenReturn(idEmissorRaiz);
        when(rs.getString(EmissorRaizRowMapper.NOME)).thenReturn(nome);
        when(rs.getString(EmissorRaizRowMapper.SITUACAO)).thenReturn(situacao);
        when(rs.getString(EmissorRaizRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(EmissorRaizRowMapper.DATAHORA)).thenReturn(datahora);
        when(rs.getLong(EmissorRaizRowMapper.ID_IMPRESSORA)).thenReturn(idImpressora);

        //call
        EmissorRaiz result = instance.mapRow(rs, row);

        verify(rs).getLong(EmissorRaizRowMapper.ID);
        verify(rs).getString(EmissorRaizRowMapper.NOME);
        verify(rs).getString(EmissorRaizRowMapper.SITUACAO);
        verify(rs).getString(EmissorRaizRowMapper.USUARIO);
        verify(rs).getTimestamp(EmissorRaizRowMapper.DATAHORA);
        verify(rs).getLong(EmissorRaizRowMapper.ID_IMPRESSORA);
        verifyNoMoreInteractions(rs);
        assertEquals(idEmissorRaiz, result.getId());
        assertEquals(nome, result.getNome());
        assertEquals(situacao, result.getSituacao());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
        assertEquals(idImpressora, result.getIdImpressora());
    }
    
}
