package com.b2wdigital.fazemu.business.client;

import br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NfeResultMsg;

/**
 *
 * @author dailton.almeida
 */
public interface ConsultarStatusServicoNFeClient {
    NfeResultMsg nfeStatusServico(String url, NfeDadosMsg nfeDadosMsg, Integer uf) throws Exception;
}
