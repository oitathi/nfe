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

import com.b2wdigital.fazemu.domain.WsMetodo;

public class WsMetodoRowMapperTest {
    @InjectMocks private WsMetodoRowMapper instance;
    @Mock private ResultSet rs;
    private final int row = 1;
    private final Long id = 2L;
    private final String nome = "_nome", descricao = "_descricao", url = "_url", action = "_action", namespace = "_namespace";
    private final String response = "_response", soap_action = "_soap_action", ws_name = "_ws_name", envelope_tag = "_envelope_tag", in_ssl = "_in_ssl";
    private final String wallet_path = "_wallet_path", wallet_password = "_wallet_password", tp_autorizacao = "_tp_autorizacao";
    private final String username = "_username", password = "_password", usuario = "_usuario", tipoRetorno = "_tipoRetorno";
    private final Timestamp dataHora = new Timestamp(3L);
    
    public WsMetodoRowMapperTest() {
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
        when(rs.getLong(WsMetodoRowMapper.ID)).thenReturn(id);
        when(rs.getString(WsMetodoRowMapper.NOME)).thenReturn(nome);
        when(rs.getString(WsMetodoRowMapper.DESCRICAO)).thenReturn(descricao);
        when(rs.getString(WsMetodoRowMapper.URL)).thenReturn(url);
        when(rs.getString(WsMetodoRowMapper.ACTION)).thenReturn(action);
        when(rs.getString(WsMetodoRowMapper.NAMESPACE)).thenReturn(namespace);
        when(rs.getString(WsMetodoRowMapper.RESPONSE)).thenReturn(response);
        when(rs.getString(WsMetodoRowMapper.SOAP_ACTION)).thenReturn(soap_action);
        when(rs.getString(WsMetodoRowMapper.WS_NAME)).thenReturn(ws_name);
        when(rs.getString(WsMetodoRowMapper.ENVELOPE_TAG)).thenReturn(envelope_tag);
        when(rs.getString(WsMetodoRowMapper.IN_SSL)).thenReturn(in_ssl);
        when(rs.getString(WsMetodoRowMapper.WALLET_PATH)).thenReturn(wallet_path);
        when(rs.getString(WsMetodoRowMapper.WALLET_PASSWORD)).thenReturn(wallet_password);
        when(rs.getString(WsMetodoRowMapper.TP_AUTORIZACAO)).thenReturn(tp_autorizacao);
        when(rs.getString(WsMetodoRowMapper.USERNAME)).thenReturn(username);
        when(rs.getString(WsMetodoRowMapper.PASSWORD)).thenReturn(password);
        when(rs.getString(WsMetodoRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(WsMetodoRowMapper.DATAHORA)).thenReturn(dataHora);
        when(rs.getString(WsMetodoRowMapper.TIPO_RETORNO)).thenReturn(tipoRetorno);
        
        //call
        WsMetodo result = instance.mapRow(rs, row);

        verify(rs).getLong(WsMetodoRowMapper.ID);
        verify(rs).getString(WsMetodoRowMapper.NOME);
        verify(rs).getString(WsMetodoRowMapper.DESCRICAO);
        verify(rs).getString(WsMetodoRowMapper.URL);
        verify(rs).getString(WsMetodoRowMapper.ACTION);
        verify(rs).getString(WsMetodoRowMapper.NAMESPACE);
        verify(rs).getString(WsMetodoRowMapper.RESPONSE);
        verify(rs).getString(WsMetodoRowMapper.SOAP_ACTION);
        verify(rs).getString(WsMetodoRowMapper.WS_NAME);
        verify(rs).getString(WsMetodoRowMapper.ENVELOPE_TAG);
        verify(rs).getString(WsMetodoRowMapper.IN_SSL);
        verify(rs).getString(WsMetodoRowMapper.WALLET_PATH);
        verify(rs).getString(WsMetodoRowMapper.WALLET_PASSWORD);
        verify(rs).getString(WsMetodoRowMapper.TP_AUTORIZACAO);
        verify(rs).getString(WsMetodoRowMapper.USERNAME);
        verify(rs).getString(WsMetodoRowMapper.PASSWORD);
        verify(rs).getString(WsMetodoRowMapper.USUARIO);
        verify(rs).getTimestamp(WsMetodoRowMapper.DATAHORA);
        verify(rs).getString(WsMetodoRowMapper.TIPO_RETORNO);
        verifyNoMoreInteractions(rs);
        assertEquals(id, result.getId());
        assertEquals(nome, result.getNome());
        assertEquals(descricao, result.getDescricao());
        assertEquals(url, result.getUrl());
        assertEquals(action, result.getAction());
        assertEquals(namespace, result.getNamespace());
        assertEquals(response, result.getResponse());
        assertEquals(soap_action, result.getSoapAction());
        assertEquals(ws_name, result.getWsName());
        assertEquals(envelope_tag, result.getEnvelopeTag());
        assertEquals(in_ssl, result.getInSsl());
        assertEquals(wallet_path, result.getWalletPath());
        assertEquals(wallet_password, result.getWalletPassword());
        assertEquals(tp_autorizacao, result.getTipoAutorizacao());
        assertEquals(username, result.getUsername());
        assertEquals(password, result.getPassword());
        assertEquals(usuario, result.getUsuario());
        assertEquals(dataHora, result.getDataHora());
        assertEquals(tipoRetorno, result.getTipoRetorno());
    }
    
}
