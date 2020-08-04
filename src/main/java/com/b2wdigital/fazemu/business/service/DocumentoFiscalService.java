package com.b2wdigital.fazemu.business.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;

import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.form.DocumentoFiscalForm;

import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;

import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.resNFe_v1.ResNFe;

public interface DocumentoFiscalService {

    List<DocumentoFiscal> listByFiltros(String tipoDocumentoFiscal,
            Long idEmissor,
            Long idDestinatario,
            Long idEstado,
            Long idMunicipio,
            String chaveAcesso,
            Long numeroDocumentoFiscal,
            Long numeroInicialDocumentoFiscal,
            Long numeroFinalDocumentoFiscal,
            Long serieDocumentoFiscal,
            Long numeroDocumentoFiscalExterno,
            Integer tipoEmissao,
            String situacaoDocumento,
            String situacao,
            Date dataHoraRegistroInicio,
            Date dataHoraRegistroFim,
            Long quantidadeRegistros);

    List<DocumentoFiscal> listByFiltros(Map<String, String> parameters) throws Exception;

    List<DocumentoFiscal> listByDateIntervalAndIdEstadoAndSituacao(String tipoDocumentoFiscal, Date dataHoraInicio, Date dataHoraFim, Long idEstado, SituacaoEnum situacao, String excludeList);

    List<DocumentoFiscal> listByDateIntervalAndIdEstadoAndSituacaoAutorizacao(String tipoDocumentoFiscal, Date dataHoraInicio, Date dataHoraFim, Long idEstado, String excludeList);

    int updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(Long idDocFiscal, String cStat, PontoDocumentoEnum pontoDocumento, SituacaoEnum situacao, Long tipoEmissao, String situacaoDocumento);

    String getSituacaoDocumentoByIdDocFiscal(Long idDocFiscal);

    DocumentoFiscal findByChaveAcesso(String chaveAcesso);

    DocumentoFiscal findById(Long idDocFiscal);

    int updateCstat(Long idDocFiscal, String cStat);

    int updateDataHora(Long idDocFiscal, Date dataHora);

    long insert(DocumentoFiscal documentoFiscal);

    String getXMLResumoNFeByChaveAcesso(String chaveAcesso);

    DocumentoFiscal criaDocumentoFiscal(DocumentoFiscalForm form) throws Exception;

    DocumentoFiscalForm persist(DocumentoFiscalForm form);

    TNfeProc getNotaFiscalCompleta(String chaveAcesso, JAXBContext context);

    ResNFe getNotaFiscalResumida(String chaveAcesso, JAXBContext context);

    List<DocumentoFiscal> listByDateIntervalAndSituacaoAndNotExistsInev(String tipoDocumentoFiscal, Date dataHoraInicio, Date dataHoraFim, SituacaoEnum situacao, String excludeList);

    List<DocumentoFiscal> listByDateIntervalAndSituacaoAndNotExistsDoev(String tipoDocumentoFiscal, Date dataHoraInicio, Date dataHoraFim, SituacaoEnum situacao, String excludeList);

}
