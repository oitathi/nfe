package com.b2wdigital.fazemu.business.service;

import java.util.List;
import java.util.Date;

import com.b2wdigital.fazemu.domain.EstadoTipoEmissao;

public interface EstadoTipoEmissaoService {
	void insert(EstadoTipoEmissao estadoTipoEmissao);
	void update(EstadoTipoEmissao estadoTipoEmissao);
	List<EstadoTipoEmissao> listByFiltros(Long idEstado, Date dataHoraInicio, Date dataHoraFim, Integer tipoEmissao);
	List<EstadoTipoEmissao> listByEstadoAndTipoEmissao(Long idEstado, Integer tipoEmissao);
        List<EstadoTipoEmissao> listByTipoEmissaoAtivo();
}
