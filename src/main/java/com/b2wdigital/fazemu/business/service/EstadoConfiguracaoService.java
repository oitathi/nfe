package com.b2wdigital.fazemu.business.service;

import java.util.List;

import com.b2wdigital.fazemu.domain.EstadoConfiguracao;

public interface EstadoConfiguracaoService {

    List<EstadoConfiguracao> listAll();

    List<EstadoConfiguracao> listByFiltros(String tipoDocumentoFiscal, Long idEstado);

    public void update(EstadoConfiguracao estadoConfiguracao);
}
