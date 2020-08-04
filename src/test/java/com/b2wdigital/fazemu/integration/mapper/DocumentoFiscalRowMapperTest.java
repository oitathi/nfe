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

import com.b2wdigital.fazemu.domain.DocumentoFiscal;

public class DocumentoFiscalRowMapperTest {
    @InjectMocks private DocumentoFiscalRowMapper instance;
    @Mock private ResultSet rs;
    private final Integer row = 2, ano = 2019;
    private final Long id = 1L, idEmissor = 3L, idDestinatario = 3L, numeroDocumentoFiscal = 6809L, serieDocumentoFiscal = 45L, numeroDocumentoFiscalExterno = 0L, idEstado = 35L, idMunicipio = 0L, tipoEmissao = 4L;
    private final String tipoDocumentoFiscal = "_tipoDocumentoFiscal", versao = "_versao", chaveAcesso = "_chaveAcesso", chaveAcessoEnviada = "_chaveAcessoEnviada";
    private final String idPonto = "_idPonto", situacaoAutorizador = "_situacaoAutorizador", usuarioReg = "_usuarioReg", usuario = "_usuario", situacaoDocumento = "_situacaoDocumento", situacao = "_situacao", idSistema = "_idSistema";
    private final Timestamp datahora = new Timestamp(11L), dataHoraEmissao = new Timestamp(12L), dataHoraReg = new Timestamp(13L);

    public DocumentoFiscalRowMapperTest() {
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
    	when(rs.getLong(DocumentoFiscalRowMapper.ID)).thenReturn(id);
        when(rs.getString(DocumentoFiscalRowMapper.TIPO_DOCUMENTO_FISCAL)).thenReturn(tipoDocumentoFiscal);
        when(rs.getLong(DocumentoFiscalRowMapper.ID_EMISSOR)).thenReturn(idEmissor);
        when(rs.getLong(DocumentoFiscalRowMapper.ID_DESTINATARIO)).thenReturn(idDestinatario);
        when(rs.getLong(DocumentoFiscalRowMapper.NRO_DOC_FISCAL)).thenReturn(numeroDocumentoFiscal);
        when(rs.getLong(DocumentoFiscalRowMapper.SERIE_DOC_FISCAL)).thenReturn(serieDocumentoFiscal);
        when(rs.getInt(DocumentoFiscalRowMapper.ANO_DOC_FISCAL)).thenReturn(ano);
        when(rs.getLong(DocumentoFiscalRowMapper.NRO_DOC_FISCAL_EXT)).thenReturn(numeroDocumentoFiscalExterno);
        when(rs.getTimestamp(DocumentoFiscalRowMapper.DT_EMISSAO)).thenReturn(dataHoraEmissao);
        when(rs.getLong(DocumentoFiscalRowMapper.ID_ESTADO)).thenReturn(idEstado);
        when(rs.getLong(DocumentoFiscalRowMapper.ID_MUNICIPIO)).thenReturn(idMunicipio);
        when(rs.getString(DocumentoFiscalRowMapper.VERSAO)).thenReturn(versao);
        when(rs.getString(DocumentoFiscalRowMapper.CHAVE_ACESSO)).thenReturn(chaveAcesso);
        when(rs.getString(DocumentoFiscalRowMapper.CHAVE_ACESSO_ENVIADA)).thenReturn(chaveAcessoEnviada);
        when(rs.getLong(DocumentoFiscalRowMapper.TIPO_EMISSAO)).thenReturn(tipoEmissao);
        when(rs.getString(DocumentoFiscalRowMapper.ID_PONTO)).thenReturn(idPonto);
        when(rs.getString(DocumentoFiscalRowMapper.SITUACAO_AUTORIZADOR)).thenReturn(situacaoAutorizador);
        when(rs.getString(DocumentoFiscalRowMapper.SITUACAO_DOC)).thenReturn(situacaoDocumento);
        when(rs.getString(DocumentoFiscalRowMapper.SITUACAO)).thenReturn(situacao);
        when(rs.getString(DocumentoFiscalRowMapper.ID_SISTEMA)).thenReturn(idSistema);
        when(rs.getString(DocumentoFiscalRowMapper.USUARIO_REG)).thenReturn(usuarioReg);
        when(rs.getTimestamp(DocumentoFiscalRowMapper.DATAHORA_REG)).thenReturn(dataHoraReg);
        when(rs.getString(DocumentoFiscalRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(DocumentoFiscalRowMapper.DATAHORA)).thenReturn(datahora);
        
        //call
        DocumentoFiscal result = instance.mapRow(rs, row);
        
        verify(rs).getLong(DocumentoFiscalRowMapper.ID);
        verify(rs).getString(DocumentoFiscalRowMapper.TIPO_DOCUMENTO_FISCAL);
        verify(rs).getLong(DocumentoFiscalRowMapper.ID_EMISSOR);
        verify(rs).getLong(DocumentoFiscalRowMapper.ID_DESTINATARIO);
        verify(rs).getLong(DocumentoFiscalRowMapper.NRO_DOC_FISCAL);
        verify(rs).getLong(DocumentoFiscalRowMapper.SERIE_DOC_FISCAL);
        verify(rs).getInt(DocumentoFiscalRowMapper.ANO_DOC_FISCAL);
        verify(rs).getLong(DocumentoFiscalRowMapper.NRO_DOC_FISCAL_EXT);
        verify(rs).getTimestamp(DocumentoFiscalRowMapper.DT_EMISSAO);
        verify(rs).getLong(DocumentoFiscalRowMapper.ID_ESTADO);
        verify(rs).getLong(DocumentoFiscalRowMapper.ID_MUNICIPIO);
        verify(rs).getString(DocumentoFiscalRowMapper.VERSAO);
        verify(rs).getString(DocumentoFiscalRowMapper.CHAVE_ACESSO);
        verify(rs).getString(DocumentoFiscalRowMapper.CHAVE_ACESSO_ENVIADA);
        verify(rs).getLong(DocumentoFiscalRowMapper.TIPO_EMISSAO);
        verify(rs).getString(DocumentoFiscalRowMapper.ID_PONTO);
        verify(rs).getString(DocumentoFiscalRowMapper.SITUACAO_AUTORIZADOR);
        verify(rs).getString(DocumentoFiscalRowMapper.SITUACAO_DOC);
        verify(rs).getString(DocumentoFiscalRowMapper.SITUACAO);
        verify(rs).getString(DocumentoFiscalRowMapper.ID_SISTEMA);
        verify(rs).getString(DocumentoFiscalRowMapper.USUARIO_REG);
        verify(rs).getTimestamp(DocumentoFiscalRowMapper.DATAHORA_REG);
        verify(rs).getString(DocumentoFiscalRowMapper.USUARIO);
        verify(rs).getTimestamp(DocumentoFiscalRowMapper.DATAHORA);
        verifyNoMoreInteractions(rs);
        assertEquals(id, result.getId());
        assertEquals(tipoDocumentoFiscal, result.getTipoDocumentoFiscal());
        assertEquals(idEmissor, result.getIdEmissor());
        assertEquals(idDestinatario, result.getIdDestinatario());
        assertEquals(numeroDocumentoFiscal, result.getNumeroDocumentoFiscal());
        assertEquals(serieDocumentoFiscal, result.getSerieDocumentoFiscal());
        assertEquals(ano, result.getAnoDocumentoFiscal());
        assertEquals(numeroDocumentoFiscalExterno, result.getNumeroDocumentoFiscalExterno());
        assertEquals(dataHoraEmissao, result.getDataHoraEmissao());
        assertEquals(idEstado, result.getIdEstado());
        assertEquals(idMunicipio, result.getIdMunicipio());
        assertEquals(versao, result.getVersao());
        assertEquals(chaveAcesso, result.getChaveAcesso());
        assertEquals(chaveAcessoEnviada, result.getChaveAcessoEnviada());
        assertEquals(tipoEmissao, result.getTipoEmissao());
        assertEquals(idPonto, result.getIdPonto());
        assertEquals(situacaoAutorizador, result.getSituacaoAutorizador());
        assertEquals(situacaoDocumento, result.getSituacaoDocumento());
        assertEquals(situacao, result.getSituacao());
        assertEquals(idSistema, result.getIdSistema());
        assertEquals(usuarioReg, result.getUsuarioReg());
        assertEquals(dataHoraReg, result.getDataHoraReg());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
    }
    
}
