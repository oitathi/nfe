package com.b2wdigital.fazemu.business.service;

import java.util.List;
import java.util.Map;

import com.b2wdigital.fazemu.domain.ResponsavelTecnico;

public interface ResponsavelTecnicoService {
	List<ResponsavelTecnico> listByFiltros(Map<String, String> parameters);
	void insert(ResponsavelTecnico responsavelTecnico);
	void update(ResponsavelTecnico responsavelTecnico);
}
