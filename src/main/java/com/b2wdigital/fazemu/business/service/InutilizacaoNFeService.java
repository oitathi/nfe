package com.b2wdigital.fazemu.business.service;

import br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeResultMsg;

/**
 *
 * @author dailton.almeida
 */
public interface InutilizacaoNFeService {

    public NfeResultMsg process(NfeDadosMsg request, String usuario) throws Exception;
}
