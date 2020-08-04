package com.b2wdigital.fazemu.business.service;

import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresse;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresseResponse;

/**
 * Distribuicao Documentos Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
public interface DistribuicaoDocumentosService {

    NfeDistDFeInteresseResponse process(NfeDistDFeInteresse.NfeDadosMsg request, int index) throws Exception;
}
