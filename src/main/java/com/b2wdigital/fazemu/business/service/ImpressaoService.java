package com.b2wdigital.fazemu.business.service;

import java.util.Map;

public interface ImpressaoService {

    void imprimirPDF(String base64, String impressora) throws Exception;

    Map<String, Boolean> listarImpressoras();
}
