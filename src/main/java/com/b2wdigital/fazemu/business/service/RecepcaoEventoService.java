package com.b2wdigital.fazemu.business.service;

import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg;

/**
 * Recepcao Evento Service.
 * 
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
public interface RecepcaoEventoService {
	NfeResultMsg process(NfeDadosMsg request, String usuario) throws Exception;
}
