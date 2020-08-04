package com.b2wdigital.fazemu.business.service;

import com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TRetConsSitNFe;

import br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfeconsultaprotocolo4.NfeResultMsg;

/**
 * Consultar Protocolo Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
public interface ConsultarProtocoloService {

    NfeResultMsg process(NfeDadosMsg request) throws Exception;

    TRetConsSitNFe consultarProtocolo(String chaveAcesso) throws Exception;

    String atualizarConsultarProtocolo(String chaveAcesso, String tipoServico, Boolean isGerarNota) throws Exception;
}
