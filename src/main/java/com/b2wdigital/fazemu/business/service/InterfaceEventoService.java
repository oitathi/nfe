package com.b2wdigital.fazemu.business.service;

import java.util.List;
import java.util.Date;

import com.b2wdigital.fazemu.domain.InterfaceEvento;

public interface InterfaceEventoService {
	List<InterfaceEvento> listByFiltros(String idSistema, Long idMetodo, String chaveAcesso, Date dataHoraRegistroInicio, Date dataHoraRegistroFim, String situacao, Long quantidadeRegistros);
}

