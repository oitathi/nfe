package com.b2wdigital.fazemu.integration.dao.jdbc;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DocumentoEpecJdbcDaoTest {
	
	private Map<String, String> parameters;
	private DocumentoEpecJdbcDao dao;
	
	@Before
	public void initialize() {
		parameters = new HashMap<String, String>();
	}
	
	@Test
	public void testaQueryIdEstado() throws Exception {
		parameters.put("idEstado", "1");
		dao = new DocumentoEpecJdbcDao();
		String query = dao.buildQuery(parameters);
		
		String expecteds = "select dpec.* from documento_epec dpec where 1 = 1" +
				" AND DPEC_ID_ESTADO = 1" ;
		Assert.assertEquals(expecteds, query);
	}
	
	@Test
	public void testaQuerySituacao() throws Exception {
		parameters.put("situacao", "L"); 
		dao = new DocumentoEpecJdbcDao();
		String query = dao.buildQuery(parameters);
		
		String expecteds = "select dpec.* from documento_epec dpec where 1 = 1 AND DPEC_SITUACAO = 'L'";
		Assert.assertEquals(expecteds, query);
	}
	
	@Test
	public void testaQueryDataInicio() throws Exception {
		parameters.put("dataHoraRegInicio", "06/11/2019 14:00:00");
		dao = new DocumentoEpecJdbcDao();
		String query = dao.buildQuery(parameters);
		String expecteds = "select dpec.* from documento_epec dpec where 1 = 1 AND DPEC_DATAHORA_REG >= TO_DATE(' 06/11/2019 14:00:00 ', 'DD/MM/YYYY HH24:MI:SS') ";
		Assert.assertEquals(expecteds, query);
	}
	
	@Test
	public void testaQueryDataFim() throws Exception {
		parameters.put("dataHoraRegFim", "08/11/2019 12:48:57");
		dao = new DocumentoEpecJdbcDao();
		String query = dao.buildQuery(parameters);
				
		String expecteds = "select dpec.* from documento_epec dpec where 1 = 1 AND DPEC_DATAHORA_REG <= TO_DATE(' 08/11/2019 12:48:57 ', 'DD/MM/YYYY HH24:MI:SS') ";
		Assert.assertEquals(expecteds, query);
	}
	
	@Test
	public void testaQueryDataInicioEDataFim() throws Exception {
		parameters.put("dataHoraRegInicio", "06/11/2019 14:00:00");
		parameters.put("dataHoraRegFim", "10/11/2019 13:00:00");
		dao = new DocumentoEpecJdbcDao();
		String query = dao.buildQuery(parameters);
		
		String expecteds = "select dpec.* from documento_epec dpec where 1 = 1 AND DPEC_DATAHORA_REG >= TO_DATE(' 06/11/2019 14:00:00 ', 'DD/MM/YYYY HH24:MI:SS')  AND DPEC_DATAHORA_REG <= TO_DATE(' 10/11/2019 13:00:00 ', 'DD/MM/YYYY HH24:MI:SS') ";
		Assert.assertEquals(expecteds, query);
	}

}
