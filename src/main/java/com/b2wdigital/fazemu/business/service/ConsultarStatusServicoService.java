package com.b2wdigital.fazemu.business.service;

import br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfestatusservico4.NfeResultMsg;
import com.b2wdigital.fazemu.domain.CodigoRetornoAutorizador;

/**
 *
 * @author dailton.almeida
 */
public interface ConsultarStatusServicoService {
    NfeResultMsg process(NfeDadosMsg request, Integer tipoEmissao) throws Exception;
    CodigoRetornoAutorizador process(Integer ufIBGE, Integer tipoEmissao, String versao) throws Exception;
}
