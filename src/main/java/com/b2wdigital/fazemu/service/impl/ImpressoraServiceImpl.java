package com.b2wdigital.fazemu.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.ImpressoraRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.ImpressoraService;
import com.b2wdigital.fazemu.domain.Impressora;

/**
 * Impressora Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class ImpressoraServiceImpl implements ImpressoraService {

    @Autowired
    ImpressoraRepository impressoraRepository;

    @Autowired
    ParametrosInfraRepository parametrosInfraRepository;

    @Override
    public List<Impressora> listByFiltros(String nome, String local, String ip, String marca, String modelo, String situacao) {
        return impressoraRepository.listByFiltros(nome, local, ip, marca, modelo, situacao);
    }

    @Override
    public void insert(Impressora impressora) {
        impressoraRepository.insert(impressora);
    }

    @Override
    public void update(Impressora impressora) {
        impressoraRepository.update(impressora);
    }

}
