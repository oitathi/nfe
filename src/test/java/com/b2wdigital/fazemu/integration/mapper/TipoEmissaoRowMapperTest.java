/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.TipoEmissao;
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
public class TipoEmissaoRowMapperTest {
    @InjectMocks private TipoEmissaoRowMapper instance;
    @Mock private ResultSet rs;
    private final int row = 1;
    private final Long id = 2L;
    private final String nome = "_nome", situacao = "_situacao", usuario = "_usuario", indicadorImpressao = "N";
    private final Timestamp dataHora = new Timestamp(3L);
    
    public TipoEmissaoRowMapperTest() {
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
        when(rs.getLong(TipoEmissaoRowMapper.ID)).thenReturn(id);
        when(rs.getString(TipoEmissaoRowMapper.NOME)).thenReturn(nome);
        when(rs.getString(TipoEmissaoRowMapper.SITUACAO)).thenReturn(situacao);
        when(rs.getString(TipoEmissaoRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(TipoEmissaoRowMapper.DATAHORA)).thenReturn(dataHora);
        when(rs.getString(TipoEmissaoRowMapper.INDICADOR_IMPRESSAO)).thenReturn(indicadorImpressao);

        //call
        TipoEmissao result = instance.mapRow(rs, row);

        verify(rs).getLong(TipoEmissaoRowMapper.ID);
        verify(rs).getString(TipoEmissaoRowMapper.NOME);
        verify(rs).getString(TipoEmissaoRowMapper.SITUACAO);
        verify(rs).getString(TipoEmissaoRowMapper.USUARIO);
        verify(rs).getTimestamp(TipoEmissaoRowMapper.DATAHORA);
        verify(rs).getString(TipoEmissaoRowMapper.INDICADOR_IMPRESSAO);
        verifyNoMoreInteractions(rs);
        assertEquals(id, result.getId());
        assertEquals(nome, result.getNome());
        assertEquals(situacao, result.getSituacao());
        assertEquals(usuario, result.getUsuario());
        assertEquals(dataHora, result.getDataHora());
        assertEquals(indicadorImpressao, result.getIndicadorImpressao());
    }
    
}
