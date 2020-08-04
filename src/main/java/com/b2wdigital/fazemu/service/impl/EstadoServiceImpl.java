package com.b2wdigital.fazemu.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.service.EstadoService;
import com.b2wdigital.fazemu.domain.Estado;

/**
 * Estado Service.
 *
 * @author Marcelo Oliveira {marcelo.doliveira@b2wdigital.com}
 * @version 1.0
 */
@Service
public class EstadoServiceImpl implements EstadoService {

    @Autowired
    private EstadoRepository estadoRepository;

    @Override
    public List<Estado> listAll() {
        return estadoRepository.listAll();
    }

    @Override
    public List<Estado> listByAtivo() {
        return estadoRepository.listByAtivo();
    }

    @Override
    public Estado findById(Long id) {
        return estadoRepository.findById(id);
    }
;

}
