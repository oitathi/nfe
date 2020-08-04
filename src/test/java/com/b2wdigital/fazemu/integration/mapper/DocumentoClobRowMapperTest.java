package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.DocumentoClob;
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
public class DocumentoClobRowMapperTest {
    @InjectMocks private DocumentoClobRowMapper instance;
    @Mock private ResultSet rs;
    private final Integer row = 2;
    private final Long id = 3L;
    private final String clob = "_clob", usuario = "_usuario";
    private final Timestamp datahora = new Timestamp(11L);
    
    
    public DocumentoClobRowMapperTest() {
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
        when(rs.getLong(DocumentoClobRowMapper.ID)).thenReturn(id);
        when(rs.getString(DocumentoClobRowMapper.CLOB)).thenReturn(clob);
        when(rs.getString(DocumentoClobRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(DocumentoClobRowMapper.DATAHORA)).thenReturn(datahora);
        
        //call
        DocumentoClob result = instance.mapRow(rs, row);
        
        verify(rs).getLong(DocumentoClobRowMapper.ID);
        verify(rs).getString(DocumentoClobRowMapper.CLOB);
        verify(rs).getString(DocumentoClobRowMapper.USUARIO);
        verify(rs).getTimestamp(DocumentoClobRowMapper.DATAHORA);
        verifyNoMoreInteractions(rs);
        assertEquals(id, result.getId());
        assertEquals(clob, result.getClob());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
    }
    
}
