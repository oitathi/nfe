package com.b2wdigital.fazemu.integration.dao.jdbc;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.reflect.Whitebox;


public class EmissorRaizJdbcDaoTest {
	
	private final String QUERY_NAME_PARAM = "select emra.* from emissor_raiz emra where 1 = 1"
			+ " and upper(emra.emra_nome) like 'B2W'"
			+ " order by emra.emra_id_emissor_raiz";
	
	private final String QUERY_SITUACAO_PARAM = "select emra.* from emissor_raiz emra where 1 = 1"
			+ " and emra.emra_situacao = 'A'"
			+ " order by emra.emra_id_emissor_raiz";
	
	private final String QUERY_ID_PARAM = "select emra.* from emissor_raiz emra where 1 = 1"
			+ " and emra.emra_id_emissor_raiz = 123"
			+ " order by emra.emra_id_emissor_raiz";
	
	private final String QUERY_ALL_PARAMS = "select emra.* from emissor_raiz emra where 1 = 1"
			+ " and upper(emra.emra_nome) like 'B2W'"
			+ " and emra.emra_situacao = 'A'"
			+ " and emra.emra_id_emissor_raiz = 123"
			+ " order by emra.emra_id_emissor_raiz";
	
	@Test
	public void testBuildQueryFilteredByNome() throws Exception {
		Map<String,String> filtro = new HashMap<String, String>();
		filtro.put("nomeEmissor", "B2W");
		String result = Whitebox.invokeMethod(new EmissorRaizJdbcDao(),"buildQuery", filtro);
		Assert.assertEquals(QUERY_NAME_PARAM, result);
	}
	
	@Test
	public void testBuildQueryFilteredBySituacao() throws Exception {
		Map<String,String> filtro = new HashMap<String, String>();
		filtro.put("situacao", "A");
		String result = Whitebox.invokeMethod(new EmissorRaizJdbcDao(),"buildQuery", filtro);
		Assert.assertEquals(QUERY_SITUACAO_PARAM, result);
	}
	
	@Test
	public void testBuildQueryFilteredById() throws Exception {
		Map<String,String> filtro = new HashMap<String, String>();
		filtro.put("id", "123");
		String result = Whitebox.invokeMethod(new EmissorRaizJdbcDao(),"buildQuery", filtro);
		Assert.assertEquals(QUERY_ID_PARAM, result);
	}
	
	@Test
	public void testBuildQueryFilteredByTodosCampos() throws Exception {
		Map<String,String> filtro = new HashMap<String, String>();
		filtro.put("nomeEmissor", "B2W");
		filtro.put("situacao", "A");
		filtro.put("id", "123");
		String result = Whitebox.invokeMethod(new EmissorRaizJdbcDao(),"buildQuery", filtro);
		Assert.assertEquals(QUERY_ALL_PARAMS, result);
	}

}
