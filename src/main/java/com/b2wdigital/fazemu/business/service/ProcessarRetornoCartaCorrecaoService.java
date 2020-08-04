package com.b2wdigital.fazemu.business.service;

import java.util.Map;

import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;

public interface ProcessarRetornoCartaCorrecaoService {
	Map<DocumentoFiscal, String> processarDocumentosCartaCorrecao(Long idLote, Object content, TipoServicoEnum documentoRetorno, String usuario) throws Exception;
	Map<DocumentoFiscal, String> liquidarCartaCorrecao(ResumoLote lote, Object content, TipoServicoEnum documentoRetorno, String usuario) throws Exception;
}
