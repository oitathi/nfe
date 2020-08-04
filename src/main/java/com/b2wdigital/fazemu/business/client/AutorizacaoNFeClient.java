package com.b2wdigital.fazemu.business.client;

import com.b2wdigital.fazemu.domain.ResumoLote;

import br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeResultMsg;

/**
 *
 * @author dailton.almeida
 */
public interface AutorizacaoNFeClient {
	NfeResultMsg nfeAutorizacaoLote(String url, NfeDadosMsg nfeDadosMsg, ResumoLote lote) throws Exception;
}
