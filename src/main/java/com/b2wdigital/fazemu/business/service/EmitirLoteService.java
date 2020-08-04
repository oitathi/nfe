package com.b2wdigital.fazemu.business.service;

import com.b2wdigital.fazemu.domain.ResumoDocumentoFiscal;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;

/**
 *
 * @author dailton.almeida
 */
public interface EmitirLoteService {
    ResumoLote emitirLote(ResumoDocumentoFiscal documentoFiscal, ServicosEnum servico, boolean isReprocessamento);
    void reconstruirLotesAbertos();
}
