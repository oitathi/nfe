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

import com.b2wdigital.fazemu.domain.EstadoConfiguracao;

public class EstadoConfiguracaoRowMapperTest {

    @InjectMocks
    private EstadoConfiguracaoRowMapper instance;
    @Mock
    ResultSet rs = null;
    private final int row = 2;
    private final Long idEstado = 3L;
    private final String tipoDocumentoFiscal = "_tipoDocumentoFiscal";
    private final String inAtivo = "_inAtivo";
    private final String inResponsavelTecnico = "_inResponsavelTecnico";
    private final String inCSRT = "_inCSRT";
    private final String inEPECAutomatico = "_inEPECAutomatico";
    private final Long quantidadeMinimaRegistros = 3L;
    private final Long periodo = 3L;
    private final Long periodoEPEC = 3L;
    private final String usuario = "_usuario";
    private final Timestamp datahora = new Timestamp(7L);

    public EstadoConfiguracaoRowMapperTest() {
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
        when(rs.getLong(EstadoConfiguracaoRowMapper.ID_ESTADO)).thenReturn(idEstado);
        when(rs.getString(EstadoConfiguracaoRowMapper.TIPO_DOCUMENTO_FISCAL)).thenReturn(tipoDocumentoFiscal);
        when(rs.getString(EstadoConfiguracaoRowMapper.IN_ATIVO)).thenReturn(inAtivo);
        when(rs.getString(EstadoConfiguracaoRowMapper.IN_RESP_TECNICO)).thenReturn(inResponsavelTecnico);
        when(rs.getString(EstadoConfiguracaoRowMapper.IN_CSRT)).thenReturn(inCSRT);
        when(rs.getString(EstadoConfiguracaoRowMapper.IN_EPEC_AUTOMATICO)).thenReturn(inEPECAutomatico);
        when(rs.getLong(EstadoConfiguracaoRowMapper.QTDE_MIN_REG)).thenReturn(quantidadeMinimaRegistros);
        when(rs.getLong(EstadoConfiguracaoRowMapper.PERIODO)).thenReturn(periodo);
        when(rs.getLong(EstadoConfiguracaoRowMapper.PERIODO_EPEC)).thenReturn(periodoEPEC);
        when(rs.getString(EstadoConfiguracaoRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(EstadoConfiguracaoRowMapper.DATAHORA)).thenReturn(datahora);

        //call
        EstadoConfiguracao result = instance.mapRow(rs, row);

        verify(rs).getLong(EstadoConfiguracaoRowMapper.ID_ESTADO);
        verify(rs).getString(EstadoConfiguracaoRowMapper.TIPO_DOCUMENTO_FISCAL);
        verify(rs).getString(EstadoConfiguracaoRowMapper.IN_ATIVO);
        verify(rs).getString(EstadoConfiguracaoRowMapper.IN_RESP_TECNICO);
        verify(rs).getString(EstadoConfiguracaoRowMapper.IN_CSRT);
        verify(rs).getString(EstadoConfiguracaoRowMapper.IN_EPEC_AUTOMATICO);
        verify(rs).getLong(EstadoConfiguracaoRowMapper.QTDE_MIN_REG);
        verify(rs).getLong(EstadoConfiguracaoRowMapper.PERIODO);
        verify(rs).getLong(EstadoConfiguracaoRowMapper.PERIODO_EPEC);
        verify(rs).getString(EstadoConfiguracaoRowMapper.USUARIO);
        verify(rs).getTimestamp(EstadoConfiguracaoRowMapper.DATAHORA);
        verifyNoMoreInteractions(rs);
        assertEquals(idEstado, result.getIdEstado());
        assertEquals(tipoDocumentoFiscal, result.getTipoDocumentoFiscal());
        assertEquals(inAtivo, result.getInAtivo());
        assertEquals(inResponsavelTecnico, result.getInResponsavelTecnico());
        assertEquals(inCSRT, result.getInCSRT());
        assertEquals(inEPECAutomatico, result.getInEPECAutomatico());
        assertEquals(quantidadeMinimaRegistros, result.getQuantidadeMinimaRegistros());
        assertEquals(periodo, result.getPeriodo());
        assertEquals(periodoEPEC, result.getPeriodoEPEC());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
    }

}
