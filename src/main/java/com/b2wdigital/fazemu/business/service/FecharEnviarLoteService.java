package com.b2wdigital.fazemu.business.service;

/**
 *
 * @author dailton.almeida
 */
public interface FecharEnviarLoteService {

    void fecharEnviarLote(Long idLote);

    void reconstruirLotesFechados();
}
