package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.AutorizadorServico;
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
public class AutorizadorServicoRowMapperTest {
    @InjectMocks private AutorizadorServicoRowMapper instance;
    @Mock private ResultSet rs;
    private final Integer row = 2;
    private final Long idAutorizador = 3L;
    private final String idServico = "_idservico", versao = "_versao", url = "_url", situacao = "_situacao", usuario = "_usuario";
    private final Timestamp datahora = new Timestamp(11L);
    
    
    public AutorizadorServicoRowMapperTest() {
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
        when(rs.getLong(AutorizadorServicoRowMapper.ID_AUTORIZADOR)).thenReturn(idAutorizador);
        when(rs.getString(AutorizadorServicoRowMapper.ID_SERVICO)).thenReturn(idServico);
        when(rs.getString(AutorizadorServicoRowMapper.VERSAO)).thenReturn(versao);
        when(rs.getString(AutorizadorServicoRowMapper.URL)).thenReturn(url);
        when(rs.getString(AutorizadorServicoRowMapper.SITUACAO)).thenReturn(situacao);
        when(rs.getString(AutorizadorServicoRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(AutorizadorServicoRowMapper.DATAHORA)).thenReturn(datahora);
        
        //call
        AutorizadorServico result = instance.mapRow(rs, row);
        
        verify(rs).getLong(AutorizadorServicoRowMapper.ID_AUTORIZADOR);
        verify(rs).getString(AutorizadorServicoRowMapper.ID_SERVICO);
        verify(rs).getString(AutorizadorServicoRowMapper.VERSAO);
        verify(rs).getString(AutorizadorServicoRowMapper.URL);
        verify(rs).getString(AutorizadorServicoRowMapper.SITUACAO);
        verify(rs).getString(AutorizadorServicoRowMapper.USUARIO);
        verify(rs).getTimestamp(AutorizadorServicoRowMapper.DATAHORA);
        verifyNoMoreInteractions(rs);
        assertEquals(idAutorizador, result.getIdAutorizador());
        assertEquals(idServico, result.getIdServico());
        assertEquals(versao, result.getVersao());
        assertEquals(url, result.getUrl());
        assertEquals(situacao, result.getSituacao());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
    }
    
}
