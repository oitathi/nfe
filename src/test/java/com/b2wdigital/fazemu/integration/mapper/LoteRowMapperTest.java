package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.Lote;
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
public class LoteRowMapperTest {

    @InjectMocks
    private LoteRowMapper instance;
    @Mock
    private ResultSet rs;
    private final Integer row = 0, situacaoAutorizacao = 13;
    private final Long idLote = 2L, tipoEmissao = 3L, idEmissorRaiz = 5L, recibo = 11L, idEstado = 35L, idMunicipio = 1L;
    private final String tpDocFiscal = "_tpdocfiscal", versao = "_versao", url = "_url", situacao = "_situacao", ur = "_ur", u = "_u", idPonto = "_idponto", servico = "_servico";
    private final Timestamp dtUltConsulta = new Timestamp(7L), dhr = new Timestamp(17L), dh = new Timestamp(19L);

    public LoteRowMapperTest() {
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
        when(rs.getLong(LoteRowMapper.ID)).thenReturn(idLote);
        when(rs.getString(LoteRowMapper.TIPO_DOCUMENTO_FISCAL)).thenReturn(tpDocFiscal);
        when(rs.getLong(LoteRowMapper.TIPO_EMISSAO)).thenReturn(tipoEmissao);
        when(rs.getLong(LoteRowMapper.ID_EMISSOR_RAIZ)).thenReturn(idEmissorRaiz);
        when(rs.getLong(LoteRowMapper.ID_ESTADO)).thenReturn(idEstado);
        when(rs.getLong(LoteRowMapper.ID_MUNICIPIO)).thenReturn(idMunicipio); //pode ser nulo
        when(rs.getString(LoteRowMapper.VERSAO)).thenReturn(versao);
        when(rs.getString(LoteRowMapper.URL)).thenReturn(url);
        when(rs.getString(LoteRowMapper.SITUACAO)).thenReturn(situacao);
        when(rs.getTimestamp(LoteRowMapper.DATA_ULTIMA_CONSULTA)).thenReturn(dtUltConsulta);
        when(rs.getLong(LoteRowMapper.RECIBO_AUTORIZADOR)).thenReturn(recibo); //pode ser nulo
        when(rs.getInt(LoteRowMapper.SITUACAO_AUTORIZADOR)).thenReturn(situacaoAutorizacao); //pode ser nulo
        when(rs.getString(LoteRowMapper.ID_PONTO)).thenReturn(idPonto);
        when(rs.getString(LoteRowMapper.TP_SERVICO)).thenReturn(servico);
        when(rs.getString(LoteRowMapper.USUARIO_REG)).thenReturn(ur);
        when(rs.getTimestamp(LoteRowMapper.DATAHORA_REG)).thenReturn(dhr);
        when(rs.getString(LoteRowMapper.USUARIO)).thenReturn(u);
        when(rs.getTimestamp(LoteRowMapper.DATAHORA)).thenReturn(dh);
        when(rs.wasNull())
                .thenReturn(Boolean.TRUE)
                .thenReturn(Boolean.TRUE);

        //call
        Lote result = instance.mapRow(rs, row);

        verify(rs).getLong(LoteRowMapper.ID);
        verify(rs).getString(LoteRowMapper.TIPO_DOCUMENTO_FISCAL);
        verify(rs).getLong(LoteRowMapper.TIPO_EMISSAO);
        verify(rs).getLong(LoteRowMapper.ID_EMISSOR_RAIZ);
        verify(rs).getLong(LoteRowMapper.ID_ESTADO);
        verify(rs).getLong(LoteRowMapper.ID_MUNICIPIO);
        verify(rs).getString(LoteRowMapper.VERSAO);
        verify(rs).getString(LoteRowMapper.URL);
        verify(rs).getString(LoteRowMapper.SITUACAO);
        verify(rs).getTimestamp(LoteRowMapper.DATA_ULTIMA_CONSULTA);
        verify(rs).getLong(LoteRowMapper.RECIBO_AUTORIZADOR); //pode ser nulo
        verify(rs).getInt(LoteRowMapper.SITUACAO_AUTORIZADOR); //pode ser nulo
        verify(rs).getString(LoteRowMapper.ID_PONTO);
        verify(rs).getString(LoteRowMapper.TP_SERVICO);
        verify(rs).getString(LoteRowMapper.USUARIO_REG);
        verify(rs).getTimestamp(LoteRowMapper.DATAHORA_REG);
        verify(rs).getString(LoteRowMapper.USUARIO);
        verify(rs).getTimestamp(LoteRowMapper.DATAHORA);
        verify(rs, times(2)).wasNull();
        verifyNoMoreInteractions(rs);
        assertEquals(idLote, result.getId());
        assertEquals(tpDocFiscal, result.getTipoDocumentoFiscal());
        assertEquals(tipoEmissao, result.getTipoEmissao());
        assertEquals(idEmissorRaiz, result.getIdEmissorRaiz());
        assertEquals(idEstado, result.getIdEstado());
        assertEquals(idMunicipio, result.getIdMunicipio());
        assertEquals(versao, result.getVersao());
        assertEquals(url, result.getUrl());
        assertEquals(situacao, result.getSituacao());
        assertEquals(dtUltConsulta, result.getDataUltimaConsulta());
        assertNull(result.getReciboAutorizacao());
        assertNull(result.getSituacaoAutorizacao());
        assertEquals(idPonto, result.getIdPonto());
        assertEquals(servico, result.getServico());
        assertEquals(ur, result.getUsuarioReg());
        assertEquals(dhr, result.getDataHoraReg());
        assertEquals(u, result.getUsuario());
        assertEquals(dh, result.getDataHora());
    }

}
