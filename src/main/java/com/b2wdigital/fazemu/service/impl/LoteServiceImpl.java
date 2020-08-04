package com.b2wdigital.fazemu.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.LoteRepository;
import com.b2wdigital.fazemu.business.service.LoteService;
import com.b2wdigital.fazemu.domain.Lote;
import com.b2wdigital.fazemu.enumeration.SituacaoLoteEnum;

/**
 * Lote Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class LoteServiceImpl implements LoteService {

    @Autowired
    private LoteRepository loteRepository;

    @Override
    public List<Lote> listByIdDocFiscalAndSituacao(Long idDocFiscal, SituacaoLoteEnum situacao) {
        return loteRepository.listByIdDocFiscalAndSituacao(idDocFiscal, situacao);
    }

    @Override
    public List<Lote> listByDateIntervalAndSituacao(Date dataHoraRegistroInicio, Date dataHoraRegistroFim, SituacaoLoteEnum situacao) {
        return loteRepository.listByDateIntervalAndSituacao(dataHoraRegistroInicio, dataHoraRegistroFim, situacao);
    }

    @Override
    public List<Lote> listByDateIntervalAndSituacaoAndIdEstado(Date dataHoraRegistroInicio, Date dataHoraRegistroFim, SituacaoLoteEnum situacao, Long idEstado) {
        return loteRepository.listByDateIntervalAndSituacaoAndIdEstado(dataHoraRegistroInicio, dataHoraRegistroFim, situacao, idEstado);
    }

    @Override
    public int cancelarLote(Long idLote, String usuario) {
        return loteRepository.cancelarLote(idLote, usuario);
    }

}
