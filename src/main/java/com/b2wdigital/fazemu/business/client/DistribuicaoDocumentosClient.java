package com.b2wdigital.fazemu.business.client;

import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresse.NfeDadosMsg;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.NfeDistDFeInteresseResponse;

/**
 * Distribuicao Documentos Client.
 * 
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
public interface DistribuicaoDocumentosClient {
	NfeDistDFeInteresseResponse nfeDistribuicaoDocumentos(String url, NfeDadosMsg nfeDadosMsg, Integer uf, Long cnpj) throws Exception;
}
