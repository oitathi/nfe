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

import com.b2wdigital.fazemu.domain.SistemaWs;

public class SistemaWsRowMapperTest {

    @InjectMocks
    private SistemaWsRowMapper instance;
    @Mock
    private ResultSet rs;
    private final int row = 1;
    private final Long id_metodo = 2L;
    private final String id_sistema = "_id_sistema", tipoDocumentoFiscal = "_tipoDocumentoFiscal", tp_servico = "_tp_servico", situacao = "_situacao", usuario = "_usuario";
    private final Timestamp dataHora = new Timestamp(3L);

    public SistemaWsRowMapperTest() {
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
        when(rs.getString(SistemaWsRowMapper.ID_SISTEMA)).thenReturn(id_sistema);
        when(rs.getString(SistemaWsRowMapper.TIPO_DOCUMENTO_FISCAL)).thenReturn(tipoDocumentoFiscal);
        when(rs.getLong(SistemaWsRowMapper.ID_METODO)).thenReturn(id_metodo);
        when(rs.getString(SistemaWsRowMapper.TIPO_SERVICO)).thenReturn(tp_servico);
        when(rs.getString(SistemaWsRowMapper.SITUACAO)).thenReturn(situacao);
        when(rs.getString(SistemaWsRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(SistemaWsRowMapper.DATAHORA)).thenReturn(dataHora);

        //call
        SistemaWs result = instance.mapRow(rs, row);

        verify(rs).getString(SistemaWsRowMapper.ID_SISTEMA);
        verify(rs).getString(SistemaWsRowMapper.TIPO_DOCUMENTO_FISCAL);
        verify(rs).getLong(SistemaWsRowMapper.ID_METODO);
        verify(rs).getString(SistemaWsRowMapper.TIPO_SERVICO);
        verify(rs).getString(SistemaWsRowMapper.SITUACAO);
        verify(rs).getString(SistemaWsRowMapper.USUARIO);
        verify(rs).getTimestamp(SistemaWsRowMapper.DATAHORA);

        verifyNoMoreInteractions(rs);

        assertEquals(id_sistema, result.getIdSistema());
        assertEquals(tipoDocumentoFiscal, result.getTipoDocumentoFiscal());
        assertEquals(id_metodo, result.getIdMetodo());
        assertEquals(tp_servico, result.getTipoServico());
        assertEquals(situacao, result.getSituacao());
        assertEquals(usuario, result.getUsuario());
        assertEquals(dataHora, result.getDataHora());
    }

}
