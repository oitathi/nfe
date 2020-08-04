package com.b2wdigital.fazemu.business.service;

import java.util.List;

import com.b2wdigital.fazemu.domain.Estado;

public interface EstadoService {

    List<Estado> listAll();

    List<Estado> listByAtivo();

    Estado findById(Long id);
}
