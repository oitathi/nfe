/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.EmissorRaizLogo;
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
public class EmissorRaizLogoRowMapperTest {
    @InjectMocks private EmissorRaizLogoRowMapper instance;
    @Mock private ResultSet rs;
    private final Integer row = 2;
    private final Long idEmissorRaiz = 3L;
    private final String idLogo = "_nome", usuario = "_usuario";
    private final Timestamp datahora = new Timestamp(5L);
    private final byte[] logo = new byte[] {0x41, 0x42, 0x43};
    
    public EmissorRaizLogoRowMapperTest() {
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
        when(rs.getLong(EmissorRaizLogoRowMapper.ID_EMISSOR_RAIZ)).thenReturn(idEmissorRaiz);
        when(rs.getString(EmissorRaizLogoRowMapper.ID_LOGO)).thenReturn(idLogo);
        when(rs.getString(EmissorRaizLogoRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(EmissorRaizLogoRowMapper.DATAHORA)).thenReturn(datahora);
        when(rs.getBytes(EmissorRaizLogoRowMapper.LOGO)).thenReturn(logo);

        //call
        EmissorRaizLogo result = instance.mapRow(rs, row);

        verify(rs).getLong(EmissorRaizLogoRowMapper.ID_EMISSOR_RAIZ);
        verify(rs).getString(EmissorRaizLogoRowMapper.ID_LOGO);
        verify(rs).getString(EmissorRaizLogoRowMapper.USUARIO);
        verify(rs).getTimestamp(EmissorRaizLogoRowMapper.DATAHORA);
        verify(rs).getBytes(EmissorRaizLogoRowMapper.LOGO);
        verifyNoMoreInteractions(rs);
        assertEquals(idEmissorRaiz, result.getIdEmissorRaiz());
        assertEquals(idLogo, result.getIdLogo());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
        assertArrayEquals(logo, result.getLogo());
    }
    
}
