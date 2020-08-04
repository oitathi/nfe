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

import com.b2wdigital.fazemu.domain.ResponsavelTecnicoCsrt;

public class ResponsavelTecnicoCsrtRowMapperTest {
    @InjectMocks private ResponsavelTecnicoCsrtRowMapper instance;
    @Mock ResultSet rs = null;
    private final int row = 2;
    private final Long id = 3L;
    private final Long idResponsavelTecnico = 4L;
    private final Long idEstado = 5L;
    private final Long idCsrt = 6L;
    private final String csrt = "_csrt";
    private final String situacao = "_situacao";
    private final String usuario = "_usuario";
    private final Timestamp datahora = new Timestamp(7L);
    
    public ResponsavelTecnicoCsrtRowMapperTest() {
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
        when(rs.getLong(ResponsavelTecnicoCsrtRowMapper.ID)).thenReturn(id);
        when(rs.getLong(ResponsavelTecnicoCsrtRowMapper.ID_RESP_TECNICO)).thenReturn(idResponsavelTecnico);
        when(rs.getLong(ResponsavelTecnicoCsrtRowMapper.ID_ESTADO)).thenReturn(idEstado);
        when(rs.getLong(ResponsavelTecnicoCsrtRowMapper.ID_CSRT)).thenReturn(idCsrt);
        when(rs.getString(ResponsavelTecnicoCsrtRowMapper.CSRT)).thenReturn(csrt);
        when(rs.getString(ResponsavelTecnicoCsrtRowMapper.SITUACAO)).thenReturn(situacao);
        when(rs.getString(ResponsavelTecnicoCsrtRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(ResponsavelTecnicoCsrtRowMapper.DATAHORA)).thenReturn(datahora);

        //call
        ResponsavelTecnicoCsrt result = instance.mapRow(rs, row);

        verify(rs).getLong(ResponsavelTecnicoCsrtRowMapper.ID);
        verify(rs).getLong(ResponsavelTecnicoCsrtRowMapper.ID_RESP_TECNICO);
        verify(rs).getLong(ResponsavelTecnicoCsrtRowMapper.ID_ESTADO);
        verify(rs).getLong(ResponsavelTecnicoCsrtRowMapper.ID_CSRT);
        verify(rs).getString(ResponsavelTecnicoCsrtRowMapper.CSRT);
        verify(rs).getString(ResponsavelTecnicoCsrtRowMapper.SITUACAO);
        verify(rs).getString(ResponsavelTecnicoCsrtRowMapper.USUARIO);
        verify(rs).getTimestamp(ResponsavelTecnicoCsrtRowMapper.DATAHORA);
        verifyNoMoreInteractions(rs);
        assertEquals(id, result.getId());
        assertEquals(idResponsavelTecnico, result.getIdResponsavelTecnico());
        assertEquals(idEstado, result.getIdEstado());
        assertEquals(idCsrt, result.getIdCsrt());
        assertEquals(csrt, result.getCsrt());
        assertEquals(situacao, result.getSituacao());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
    }
    
}
