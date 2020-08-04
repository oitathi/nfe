package com.b2wdigital.fazemu.business.client;

import com.b2wdigital.fazemu.domain.ResumoLote;

import br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeinutilizacao4.NfeResultMsg;

/**
 *
 * @author dailton.almeida
 */
public interface InutilizacaoNFeClient {
    NfeResultMsg nfeInutilizacao(String url, ResumoLote lote, NfeDadosMsg nfeDadosMsg);
}
