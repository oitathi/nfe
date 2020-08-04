package com.b2wdigital.fazemu.business.service;

import java.util.List;
import java.util.Date;

import com.b2wdigital.fazemu.domain.Lote;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;

public interface LoteService {
	List<Lote> listByIdDocFiscalAndSituacao(Long idDocFiscal, SituacaoLoteEnum situacao);

	List<Lote> listByDateIntervalAndSituacao(Date dataHoraRegistroInicio, Date dataHoraRegistroFim, SituacaoLoteEnum situacao);

	List<Lote> listByDateIntervalAndSituacaoAndIdEstado(Date dataHoraRegistroInicio, Date dataHoraRegistroFim, SituacaoLoteEnum situacao, Long idEstado);

	int cancelarLote(Long idLote, String usuario);
}
