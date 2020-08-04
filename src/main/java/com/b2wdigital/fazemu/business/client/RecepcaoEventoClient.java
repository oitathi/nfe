package com.b2wdigital.fazemu.business.client;

import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;

import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nferecepcaoevento4.NfeResultMsg;

/**
 * Recepcao Evento Client.
 * 
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
public interface RecepcaoEventoClient {
	NfeResultMsg nfeRecepcaoEvento(String url, NfeDadosMsg nfeDadosMsg, ResumoLote lote, ServicosEnum servico) throws Exception;
}
