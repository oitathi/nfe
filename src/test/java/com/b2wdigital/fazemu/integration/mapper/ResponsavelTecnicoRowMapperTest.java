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

import com.b2wdigital.fazemu.domain.ResponsavelTecnico;

public class ResponsavelTecnicoRowMapperTest {
    @InjectMocks private ResponsavelTecnicoRowMapper instance;
    @Mock ResultSet rs = null;
    private final int row = 2;
    private final Long idResponsavelTecnico = 3L;
    private final Long idEmissorRaiz = 1L;
    private final Long cnpj = 4L;
    private final String contato = "_contato";
    private final String email = "_email";
    private final Long telefone = 5L;
    private final String situacao = "_situacao";
    private final String usuario = "_usuario";
    private final Timestamp datahora = new Timestamp(7L);
    
    public ResponsavelTecnicoRowMapperTest() {
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
        when(rs.getLong(ResponsavelTecnicoRowMapper.ID)).thenReturn(idResponsavelTecnico);
        when(rs.getLong(ResponsavelTecnicoRowMapper.ID_EMISSOR_RAIZ)).thenReturn(idEmissorRaiz);
        when(rs.getLong(ResponsavelTecnicoRowMapper.CNPJ)).thenReturn(cnpj);
        when(rs.getString(ResponsavelTecnicoRowMapper.CONTATO)).thenReturn(contato);
        when(rs.getString(ResponsavelTecnicoRowMapper.EMAIL)).thenReturn(email);
        when(rs.getLong(ResponsavelTecnicoRowMapper.TELEFONE)).thenReturn(telefone);
        when(rs.getString(ResponsavelTecnicoRowMapper.SITUACAO)).thenReturn(situacao);
        when(rs.getString(ResponsavelTecnicoRowMapper.USUARIO)).thenReturn(usuario);
        when(rs.getTimestamp(ResponsavelTecnicoRowMapper.DATAHORA)).thenReturn(datahora);
       
        
        //call
        ResponsavelTecnico result = instance.mapRow(rs, row);

        verify(rs).getLong(ResponsavelTecnicoRowMapper.ID);
        verify(rs).getLong(ResponsavelTecnicoRowMapper.ID_EMISSOR_RAIZ);
        verify(rs).getLong(ResponsavelTecnicoRowMapper.CNPJ);
        verify(rs).getString(ResponsavelTecnicoRowMapper.CONTATO);
        verify(rs).getString(ResponsavelTecnicoRowMapper.EMAIL);
        verify(rs).getLong(ResponsavelTecnicoRowMapper.TELEFONE);
        verify(rs).getString(ResponsavelTecnicoRowMapper.SITUACAO);
        verify(rs).getString(ResponsavelTecnicoRowMapper.USUARIO);
        verify(rs).getTimestamp(ResponsavelTecnicoRowMapper.DATAHORA);
        verifyNoMoreInteractions(rs);
        assertEquals(idResponsavelTecnico, result.getIdResponsavelTecnico());
        assertEquals(idEmissorRaiz, result.getIdEmissorRaiz());
        assertEquals(cnpj, result.getCnpj());
        assertEquals(contato, result.getContato());
        assertEquals(email, result.getEmail());
        assertEquals(telefone, result.getTelefone());
        assertEquals(situacao, result.getSituacao());
        assertEquals(usuario, result.getUsuario());
        assertEquals(datahora, result.getDataHora());
    }
    
}
