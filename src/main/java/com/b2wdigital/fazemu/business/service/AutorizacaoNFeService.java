package com.b2wdigital.fazemu.business.service;

import br.inf.portalfiscal.nfe.wsdl.nfeautorizacao4.NfeDadosMsg;

/**
 * Autorizacao NFe Service.
 * 
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
public interface AutorizacaoNFeService {
	void process(NfeDadosMsg request) throws Exception;
}
