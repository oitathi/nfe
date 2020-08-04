package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.DocumentoLote;
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
public class DocumentoLoteRowMapperTest {
    @InjectMocks private DocumentoLoteRowMapper instance;
    @Mock private ResultSet rs;
    private final Integer row = 2;
    private final Long idDocFiscal = 3L, idLote = 5L, idXml = 7L;
    private final String usuario = "_usuario";
    private final Timestamp datahora = new Timestamp(11L);
    
    
    public DocumentoLoteRowMapperTest() {
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
        when(rs.getLong(DocumentoLoteRowMapper.ID_DOCUMENTO_FISCAL)).thenReturn(idDocFiscal);
        when(rs.getLong(DocumentoLoteRowMapper.ID_LOTE)).thenReturn(idLote);
        when(rs.getLong(DocumentoLoteRowMapper.ID_XML)).thenReturn(idXml);
        when(rs.getString(DocumentoLoteRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(DocumentoLoteRowMapper.DATAHORA)).thenReturn(datahora);
        
        //call
        DocumentoLote result = instance.mapRow(rs, row);
        
        verify(rs).getLong(DocumentoLoteRowMapper.ID_DOCUMENTO_FISCAL);
        verify(rs).getLong(DocumentoLoteRowMapper.ID_LOTE);
        verify(rs).getLong(DocumentoLoteRowMapper.ID_XML);
        verify(rs).getString(DocumentoLoteRowMapper.USUARIO);
        verify(rs).getTimestamp(DocumentoLoteRowMapper.DATAHORA);
        verifyNoMoreInteractions(rs);
        assertEquals(idDocFiscal, result.getIdDocumentoFiscal());
        assertEquals(idLote, result.getIdLote());
        assertEquals(idXml, result.getIdXml());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
    }
    
}
