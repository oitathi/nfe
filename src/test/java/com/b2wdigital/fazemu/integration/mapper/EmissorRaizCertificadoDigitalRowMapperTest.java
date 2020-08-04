/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.integration.mapper;

import com.b2wdigital.fazemu.domain.EmissorRaizCertificadoDigital;
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
public class EmissorRaizCertificadoDigitalRowMapperTest {
    @InjectMocks private EmissorRaizCertificadoDigitalRowMapper instance;
    @Mock private ResultSet rs;
//    @Mock private Blob blob;
    private final int row = 0;
    private final Long idCertificado = 2L;
    private final Long idEmissorRaiz = 7L;
    private final Timestamp dataVigenciaInicio = new Timestamp(3L);
    private final Timestamp dataVigenciaFim = new Timestamp(5L);
    private final String usuario = "_usuario";
    private final Timestamp dataHora = new Timestamp(11L);
    private final byte[] bytes = new byte[] {0x40, 0x41, 0x42};
    private final String senha = "_senha";

    public EmissorRaizCertificadoDigitalRowMapperTest() {
    }
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
//        DefaultLobHandler lobHandler = new DefaultLobHandler();
//        lobHandler.setWrapAsLob(false);
//        ReflectionTestUtils.setField(instance, "lobHandler", lobHandler);
    }
    
    @After
    public void tearDown() {
    }
//		result.setId(rs.getLong(ID));
//		result.setIdEmissorRaiz(rs.getLong(ID_EMISSOR_RAIZ));
//		result.setDataVigenciaInicio(rs.getTimestamp(DATA_VIGENCIA_INICIO));
//		result.setDataVigenciaFim(rs.getTimestamp(DATA_VIGENCIA_FIM));
//		result.setUsuario(rs.getString(USUARIO));
//		result.setDataHora(rs.getTimestamp(DATAHORA));
//		result.setCertificadoBytes(lobHandler.getBlobAsBytes(rs, CERTIFICADO));
//		result.setSenha(rs.getString(SENHA));

    @Test
    public void testMapRowNonNullBytes() throws Exception {
        when(rs.getLong(EmissorRaizCertificadoDigitalRowMapper.ID)).thenReturn(idCertificado);
        when(rs.getLong(EmissorRaizCertificadoDigitalRowMapper.ID_EMISSOR_RAIZ)).thenReturn(idEmissorRaiz);
        when(rs.getTimestamp(EmissorRaizCertificadoDigitalRowMapper.DATA_VIGENCIA_INICIO)).thenReturn(dataVigenciaInicio);
        when(rs.getTimestamp(EmissorRaizCertificadoDigitalRowMapper.DATA_VIGENCIA_FIM)).thenReturn(dataVigenciaFim);
        when(rs.getString(EmissorRaizCertificadoDigitalRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(EmissorRaizCertificadoDigitalRowMapper.DATAHORA)).thenReturn(dataHora);
        when(rs.getBytes(EmissorRaizCertificadoDigitalRowMapper.CERTIFICADO)).thenReturn(bytes);
        when(rs.getString(EmissorRaizCertificadoDigitalRowMapper.SENHA)).thenReturn(senha);
        
        //call
        EmissorRaizCertificadoDigital result = instance.mapRow(rs, row);
        
        verify(rs).getLong(EmissorRaizCertificadoDigitalRowMapper.ID);
        verify(rs).getLong(EmissorRaizCertificadoDigitalRowMapper.ID_EMISSOR_RAIZ);
        verify(rs).getTimestamp(EmissorRaizCertificadoDigitalRowMapper.DATA_VIGENCIA_INICIO);
        verify(rs).getTimestamp(EmissorRaizCertificadoDigitalRowMapper.DATA_VIGENCIA_FIM);
        verify(rs).getString(EmissorRaizCertificadoDigitalRowMapper.USUARIO);
        verify(rs).getTimestamp(EmissorRaizCertificadoDigitalRowMapper.DATAHORA);
        verify(rs).getBytes(EmissorRaizCertificadoDigitalRowMapper.CERTIFICADO);
        verify(rs).getString(EmissorRaizCertificadoDigitalRowMapper.SENHA);
        verifyNoMoreInteractions(rs);
        assertEquals(idCertificado, result.getId());
        assertEquals(idEmissorRaiz, result.getIdEmissorRaiz());
        assertEquals(dataVigenciaInicio, result.getDataVigenciaInicio());
        assertEquals(dataVigenciaFim, result.getDataVigenciaFim());
        assertEquals(usuario, result.getUsuario());
        assertArrayEquals(bytes, result.getCertificadoBytes());
        assertEquals(senha, result.getSenha());
    }
    @Test
    public void testMapRowNullBytes() throws Exception {
        when(rs.getLong(EmissorRaizCertificadoDigitalRowMapper.ID)).thenReturn(idCertificado);
        when(rs.getLong(EmissorRaizCertificadoDigitalRowMapper.ID_EMISSOR_RAIZ)).thenReturn(idEmissorRaiz);
        when(rs.getTimestamp(EmissorRaizCertificadoDigitalRowMapper.DATA_VIGENCIA_INICIO)).thenReturn(dataVigenciaInicio);
        when(rs.getTimestamp(EmissorRaizCertificadoDigitalRowMapper.DATA_VIGENCIA_FIM)).thenReturn(dataVigenciaFim);
        when(rs.getString(EmissorRaizCertificadoDigitalRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(EmissorRaizCertificadoDigitalRowMapper.DATAHORA)).thenReturn(dataHora);
        when(rs.getBytes(EmissorRaizCertificadoDigitalRowMapper.CERTIFICADO)).thenReturn(null);
        when(rs.getString(EmissorRaizCertificadoDigitalRowMapper.SENHA)).thenReturn(senha);
        
        //call
        EmissorRaizCertificadoDigital result = instance.mapRow(rs, row);
        
        verify(rs).getLong(EmissorRaizCertificadoDigitalRowMapper.ID);
        verify(rs).getLong(EmissorRaizCertificadoDigitalRowMapper.ID_EMISSOR_RAIZ);
        verify(rs).getTimestamp(EmissorRaizCertificadoDigitalRowMapper.DATA_VIGENCIA_INICIO);
        verify(rs).getTimestamp(EmissorRaizCertificadoDigitalRowMapper.DATA_VIGENCIA_FIM);
        verify(rs).getString(EmissorRaizCertificadoDigitalRowMapper.USUARIO);
        verify(rs).getTimestamp(EmissorRaizCertificadoDigitalRowMapper.DATAHORA);
        verify(rs).getBytes(EmissorRaizCertificadoDigitalRowMapper.CERTIFICADO);
        verify(rs).getString(EmissorRaizCertificadoDigitalRowMapper.SENHA);
        verifyNoMoreInteractions(rs);
        assertEquals(idCertificado, result.getId());
        assertEquals(idEmissorRaiz, result.getIdEmissorRaiz());
        assertEquals(dataVigenciaInicio, result.getDataVigenciaInicio());
        assertEquals(dataVigenciaFim, result.getDataVigenciaFim());
        assertEquals(usuario, result.getUsuario());
        assertNull(result.getCertificadoBytes());
        assertEquals(senha, result.getSenha());
    }
    
}
