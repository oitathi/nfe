package com.b2wdigital.fazemu.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.ResponsavelTecnicoRepository;
import com.b2wdigital.fazemu.business.service.ResponsavelTecnicoService;
import com.b2wdigital.fazemu.domain.ResponsavelTecnico;

/**
 * ResponsavelTecnico Service.
 *
 * @author Marcelo Oliveira {marcelo.doliveira@b2wdigital.com}
 * @version 1.0
 */
@Service
public class ResponsavelTecnicoServiceImpl implements ResponsavelTecnicoService {

    @Autowired
    private ResponsavelTecnicoRepository responsavelTecnicoRepository;

    @Override
    public List<ResponsavelTecnico> listByFiltros(Map<String, String> parameters) {
        return responsavelTecnicoRepository.listByFiltros(parameters);
    }

    @Override
    public void insert(ResponsavelTecnico responsavelTecnico) {
        responsavelTecnicoRepository.insert(responsavelTecnico);
    }

    @Override
    public void update(ResponsavelTecnico responsavelTecnico) {
        responsavelTecnicoRepository.update(responsavelTecnico);
    }

}
