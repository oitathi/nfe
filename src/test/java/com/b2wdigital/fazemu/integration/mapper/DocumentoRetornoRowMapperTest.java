/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.DocumentoRetorno;
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
public class DocumentoRetornoRowMapperTest {

    @InjectMocks
    private DocumentoRetornoRowMapper instance;
    @Mock
    private ResultSet rs;
    private final Integer row = 2;
    private final Long idDocFiscal = 3L, idXml = 5L;
    private final String tipoServico = "AUTR", url = "url", usuarioReg = "_usuarioreg", usuario = "_usuario";
    private final Timestamp datahoraReg = new Timestamp(7L), datahora = new Timestamp(11L);

    public DocumentoRetornoRowMapperTest() {
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
        when(rs.getLong(DocumentoRetornoRowMapper.ID_DOC_FISCAL)).thenReturn(idDocFiscal);
        when(rs.getString(DocumentoRetornoRowMapper.TIPO_SERVICO)).thenReturn(tipoServico);
        when(rs.getLong(DocumentoRetornoRowMapper.ID_XML)).thenReturn(idXml);
        when(rs.getString(DocumentoRetornoRowMapper.USUARIO_REG)).thenReturn(usuarioReg);
        when(rs.getTimestamp(DocumentoRetornoRowMapper.DATAHORA_REG)).thenReturn(datahoraReg);
        when(rs.getString(DocumentoRetornoRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(DocumentoRetornoRowMapper.DATAHORA)).thenReturn(datahora);
        when(rs.getString(DocumentoRetornoRowMapper.URL)).thenReturn(url);

        //call
        DocumentoRetorno result = instance.mapRow(rs, row);
//        verify(rs).getLong(DocumentoRetornoRowMapper.ID_DOC_FISCAL);
//        verify(rs).getString(DocumentoRetornoRowMapper.TP_SERVICO);
//        verify(rs).getLong(DocumentoRetornoRowMapper.ID_XML);
//        verify(rs).getString(DocumentoRetornoRowMapper.USUARIO_REG);
//        verify(rs).getTimestamp(DocumentoRetornoRowMapper.DATAHORA_REG);
//        verify(rs).getString(DocumentoRetornoRowMapper.USUARIO);
//        verify(rs).getTimestamp(DocumentoRetornoRowMapper.DATAHORA);
//        verifyNoMoreInteractions(rs);
        assertEquals(idDocFiscal, result.getIdDocumentoFiscal());
        assertEquals(tipoServico, result.getTipoServico());
        assertEquals(idXml, result.getIdXml());
        assertEquals(usuarioReg, result.getUsuarioReg());
        assertEquals(datahoraReg, result.getDataHoraReg());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
        assertEquals(url, result.getUrl());
    }

}
