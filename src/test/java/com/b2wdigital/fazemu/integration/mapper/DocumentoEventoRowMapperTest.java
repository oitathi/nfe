package com.b2wdigital.fazemu.integration.mapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.b2wdigital.fazemu.domain.DocumentoEvento;

public class DocumentoEventoRowMapperTest {
    @InjectMocks private DocumentoEventoRowMapper instance;
    @Mock private ResultSet rs;
    private final Integer row = 2;
    private final Long id = 1L, idDocFiscal = 3L, idXml = 7L;
    private final String usuario = "_usuario", idPonto = "DRAW", tipoServico = "AUTR", situacaoAutorizador = "100";
    private final Timestamp datahora = new Timestamp(11L);
    
    
    public DocumentoEventoRowMapperTest() {
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
    	when(rs.getLong(DocumentoEventoRowMapper.ID)).thenReturn(id);
        when(rs.getLong(DocumentoEventoRowMapper.ID_DOCUMENTO_FISCAL)).thenReturn(idDocFiscal);
        when(rs.getString(DocumentoEventoRowMapper.ID_PONTO)).thenReturn(idPonto);
        when(rs.getString(DocumentoEventoRowMapper.TIPO_SERVICO)).thenReturn(tipoServico);
        when(rs.getString(DocumentoEventoRowMapper.SITUACAO_AUTORIZADOR)).thenReturn(situacaoAutorizador);
        when(rs.getLong(DocumentoEventoRowMapper.ID_XML)).thenReturn(idXml);
        when(rs.getString(DocumentoEventoRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(DocumentoEventoRowMapper.DATAHORA)).thenReturn(datahora);
        
        //call
        DocumentoEvento result = instance.mapRow(rs, row);
        
        verify(rs).getLong(DocumentoEventoRowMapper.ID);
        verify(rs).getLong(DocumentoEventoRowMapper.ID_DOCUMENTO_FISCAL);
        verify(rs).getString(DocumentoEventoRowMapper.ID_PONTO);
        verify(rs).getString(DocumentoEventoRowMapper.TIPO_SERVICO);
        verify(rs).getString(DocumentoEventoRowMapper.SITUACAO_AUTORIZADOR);
        verify(rs).getLong(DocumentoEventoRowMapper.ID_XML);
        verify(rs).getString(DocumentoEventoRowMapper.USUARIO);
        verify(rs).getTimestamp(DocumentoEventoRowMapper.DATAHORA);
        verifyNoMoreInteractions(rs);
        assertEquals(id, result.getId());
        assertEquals(idDocFiscal, result.getIdDocumentoFiscal());
        assertEquals(idPonto, result.getIdPonto());
        assertEquals(tipoServico, result.getTipoServico());
        assertEquals(situacaoAutorizador, result.getSituacaoAutorizador());
        assertEquals(idXml, result.getIdXml());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
    }
    
}
