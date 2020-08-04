package com.b2wdigital.fazemu.integration.dao.jdbc;

import com.b2wdigital.fazemu.domain.DocumentoRetorno;
import com.b2wdigital.fazemu.integration.mapper.DocumentoRetornoRowMapper;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 *
 * @author dailton.almeida
 */
public class DocumentoRetornoJdbcDaoTest {

    private EmbeddedDatabase db;
    private DocumentoRetornoJdbcDao instance;
    private final Long idDocFiscal = 2L;
    private final Long idXml = 40L;
    private final String usuarioReg = "UR";
    private final String usuario = "U";

    public DocumentoRetornoJdbcDaoTest() {
    }

    @Before
    public void setUp() {
        db = new EmbeddedDatabaseBuilder()
                .addScript("db/documentoretornodao_createtable.sql")
                .addScript("db/documentoretornodao_insertdata.sql")
                .build();

        instance = new DocumentoRetornoJdbcDao();
        ReflectionTestUtils.setField(instance, "namedParameterJdbcOperations", new NamedParameterJdbcTemplate(db));
        ReflectionTestUtils.setField(instance, "documentoRetornoRowMapper", new DocumentoRetornoRowMapper());
    }

    @After
    public void tearDown() {
        db.shutdown();
    }

    @Test
    public void testFindByIdDocFiscalAndTpServicoAndTpEvento() {
        String tpServico = "AUTR";

        //call
        DocumentoRetorno result = instance.findByIdDocFiscalAndTpServicoAndTpEvento(idDocFiscal, tpServico, null);

        assertEquals((Long) 20L, result.getIdXml());
    }

    @Test
    public void testFindByIdDocFiscal() {
        //call
        List<DocumentoRetorno> result = instance.findByIdDocFiscal(idDocFiscal);

        assertEquals(1, result.size());
        assertEquals("AUTR", result.get(0).getTipoServico());
        assertEquals((Long) 20L, result.get(0).getIdXml());
    }

    @Test
    public void testInsert() {
        String tpServico = "AUTR"; //novo

        //call
        int result = instance.insert(idDocFiscal, tpServico, null, idXml, usuarioReg, usuario);

        assertEquals(1, result);
        List<DocumentoRetorno> dretList = instance.findByIdDocFiscal(idDocFiscal);
        assertEquals(2, dretList.size());
    }

    @Test
    public void testUpdate() {
        String tpServico = "AUTR"; //existente

        //call
        int result = instance.update(idDocFiscal, tpServico, null, idXml, usuario);

        assertEquals(1, result);
        List<DocumentoRetorno> dretList = instance.findByIdDocFiscal(idDocFiscal);
        assertEquals(1, dretList.size());
        assertEquals(idXml, dretList.get(0).getIdXml());
    }

}
