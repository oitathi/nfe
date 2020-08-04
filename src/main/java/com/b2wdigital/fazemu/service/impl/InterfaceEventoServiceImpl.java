package com.b2wdigital.fazemu.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.InterfaceEventoRepository;
import com.b2wdigital.fazemu.business.service.InterfaceEventoService;
import com.b2wdigital.fazemu.domain.InterfaceEvento;

/**
 * Interface Evento Service.
 *
 * @author Marcelo Oliveira {marcelo.doliveira@b2wdigital.com}
 * @version 1.0
 */
@Service
public class InterfaceEventoServiceImpl implements InterfaceEventoService {

    @Autowired
    private InterfaceEventoRepository interfaceEventoRepository;

    @Override
    public List<InterfaceEvento> listByFiltros(String idSistema, Long idMetodo, String chaveAcesso, Date dataHoraRegistroInicio, Date dataHoraRegistroFim, String situacao, Long quantidadeRegistros) {
        return interfaceEventoRepository.listByFiltros(idSistema, idMetodo, chaveAcesso, dataHoraRegistroInicio, dataHoraRegistroFim, situacao, quantidadeRegistros);
    }

}
