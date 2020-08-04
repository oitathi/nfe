package com.b2wdigital.fazemu.business.service;

import java.util.List;
import java.util.Map;

import com.b2wdigital.fazemu.domain.TipoEmissao;
import com.b2wdigital.fazemu.domain.form.TipoEmissaoForm;

/**
 * Tipo Emissao Service.
 * 
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
public interface TipoEmissaoService {
	Integer getTipoEmissaoByCodigoIBGE(String codigoIBGE);

	List<TipoEmissaoForm> listAll();

	List<TipoEmissao> listAtivos();
	
	List<TipoEmissao> listByIdEstado(Map<String, String> parameters)throws Exception;
	
	TipoEmissao findByIdTipoEmissao(Long idTipoEmissao);
}
