package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.CodigoRetornoAutorizador;
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
public class CodigoRetornoAutorizadorRowMapperTest {
    @InjectMocks private CodigoRetornoAutorizadorRowMapper instance;
    @Mock private ResultSet rs;
    private final Integer row = 0, id = 2;
    private final String tpDocFiscal = "_tpdocfiscal", descricao = "_desc", situacaoAutorizador = "_sa", u = "_u";
    private final Timestamp dh = new Timestamp(19L);
    
    public CodigoRetornoAutorizadorRowMapperTest() {
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
        when(rs.getInt(CodigoRetornoAutorizadorRowMapper.ID)).thenReturn(id);
        when(rs.getString(CodigoRetornoAutorizadorRowMapper.TIPO_DOCUMENTO_FISCAL)).thenReturn(tpDocFiscal);
        when(rs.getString(CodigoRetornoAutorizadorRowMapper.DESCRICAO)).thenReturn(descricao);
        when(rs.getString(CodigoRetornoAutorizadorRowMapper.SITUACAO_AUTORIZADOR)).thenReturn(situacaoAutorizador); //pode ser nulo
        when(rs.getString(CodigoRetornoAutorizadorRowMapper.USUARIO)).thenReturn(u);
        when(rs.getTimestamp(CodigoRetornoAutorizadorRowMapper.DATAHORA)).thenReturn(dh);
        
        //call
        CodigoRetornoAutorizador result = instance.mapRow(rs, row);

        verify(rs).getInt(CodigoRetornoAutorizadorRowMapper.ID);
        verify(rs).getString(CodigoRetornoAutorizadorRowMapper.TIPO_DOCUMENTO_FISCAL);
        verify(rs).getString(CodigoRetornoAutorizadorRowMapper.DESCRICAO);
        verify(rs).getString(CodigoRetornoAutorizadorRowMapper.SITUACAO_AUTORIZADOR); //pode ser nulo
        verify(rs).getString(CodigoRetornoAutorizadorRowMapper.USUARIO);
        verify(rs).getTimestamp(CodigoRetornoAutorizadorRowMapper.DATAHORA);
        verifyNoMoreInteractions(rs);
        assertEquals(id, result.getId());
        assertEquals(tpDocFiscal, result.getTipoDocumentoFiscal());
        assertEquals(descricao, result.getDescricao());
        assertEquals(situacaoAutorizador, result.getSituacaoAutorizador());
        assertEquals(u, result.getUsuario());
        assertEquals(dh, result.getDataHora());
    }
    
}
