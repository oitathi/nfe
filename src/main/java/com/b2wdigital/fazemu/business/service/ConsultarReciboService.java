package com.b2wdigital.fazemu.business.service;

import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe;
import java.util.Map;
import javax.xml.bind.JAXBException;

/**
 *
 * @author marcelo.doliveira
 */
public interface ConsultarReciboService {

    void consultarReciboCallback(ResumoLote lote);

    Map<DocumentoFiscal, String> consultarRecibo(ResumoLote lote);

    String encaixarProtocolo(String xmlNFe, TProtNFe protNFe, DocumentoFiscal docu, String usuario) throws JAXBException;

    void reconstruirLotesEnviados();
}
