package com.b2wdigital.fazemu.business.client;

import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;

import br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeResultMsg;

/**
 * Consultar Protocolo Client.
 * 
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
public interface ConsultarProtocoloClient {
	
	NfeResultMsg nfeConsultarProtocolo(String url, ChaveAcessoNFe chaveAcesso, String versao, NfeDadosMsg nfeDadosMsg) throws Exception;
}
