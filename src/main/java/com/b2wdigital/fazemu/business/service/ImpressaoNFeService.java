package com.b2wdigital.fazemu.business.service;

import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.form.ImpressaoNFeForm;

public interface ImpressaoNFeService {

    void imprimirComNFe(ImpressaoNFeForm form) throws Exception;

    void imprimirSemNFe(String nfe) throws Exception;

//    void imprimirEPEC(String nfe) throws Exception;
    void segundaViaImpressao(DocumentoFiscal docu, Integer tipoEmissao);
}
