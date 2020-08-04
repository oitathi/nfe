package com.b2wdigital.fazemu.business.service;

import java.util.List;

import com.b2wdigital.fazemu.domain.CodigoRetornoAutorizador;
import com.b2wdigital.fazemu.domain.form.CodigoRetornoAutorizadorForm;

public interface CodigoRetornoAutorizadorService {

    List<CodigoRetornoAutorizadorForm> listAll();

    List<CodigoRetornoAutorizadorForm> listByCodigo(Integer cstat);

    void insert(CodigoRetornoAutorizador codigoRetornoAutorizador);

    void update(CodigoRetornoAutorizador codigoRetornoAutorizador);

    String getSituacaoAutorizacaoById(Integer cStat);
}
