/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.Autorizador;
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
public class AutorizadorRowMapperTest {
    @InjectMocks private AutorizadorRowMapper instance;
    @Mock private ResultSet rs;
    private final Integer row = 2;
    private final Long idAutorizador = 3L;
    private final String codigoExterno = "_codigoexterno", tpDocFiscal = "_tpdocfis", nome = "_nome", situacao = "_situacao", usuario = "_usuario";
    private final Timestamp datahora = new Timestamp(11L);
    
    
    public AutorizadorRowMapperTest() {
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
        when(rs.getLong(AutorizadorRowMapper.ID)).thenReturn(idAutorizador);
        when(rs.getString(AutorizadorRowMapper.CODIGO_EXTERNO)).thenReturn(codigoExterno);
        when(rs.getString(AutorizadorRowMapper.TIPO_DOCUMENTO_FISCAL)).thenReturn(tpDocFiscal);
        when(rs.getString(AutorizadorRowMapper.NOME)).thenReturn(nome);
        when(rs.getString(AutorizadorRowMapper.SITUACAO)).thenReturn(situacao);
        when(rs.getString(AutorizadorRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(AutorizadorRowMapper.DATAHORA)).thenReturn(datahora);
        
        //call
        Autorizador result = instance.mapRow(rs, row);
        
        verify(rs).getLong(AutorizadorRowMapper.ID);
        verify(rs).getString(AutorizadorRowMapper.CODIGO_EXTERNO);
        verify(rs).getString(AutorizadorRowMapper.TIPO_DOCUMENTO_FISCAL);
        verify(rs).getString(AutorizadorRowMapper.NOME);
        verify(rs).getString(AutorizadorRowMapper.SITUACAO);
        verify(rs).getString(AutorizadorRowMapper.USUARIO);
        verify(rs).getTimestamp(AutorizadorRowMapper.DATAHORA);
        verifyNoMoreInteractions(rs);
        assertEquals(idAutorizador, result.getId());
        assertEquals(codigoExterno, result.getCodigoExterno());
        assertEquals(tpDocFiscal, result.getTipoDocumentoFiscal());
        assertEquals(nome, result.getNome());
        assertEquals(situacao, result.getSituacao());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
    }
    
}
