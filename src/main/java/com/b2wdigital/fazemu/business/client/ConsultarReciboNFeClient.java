package com.b2wdigital.fazemu.business.client;

import com.b2wdigital.fazemu.domain.ResumoLote;

import br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nferetautorizacao4.NfeResultMsg;

/**
 *
 * @author dailton.almeida
 */
public interface ConsultarReciboNFeClient {
    NfeResultMsg nfeRetAutorizacaoLote(String url, NfeDadosMsg nfeDadosMsg, ResumoLote lote) throws Exception;
}
