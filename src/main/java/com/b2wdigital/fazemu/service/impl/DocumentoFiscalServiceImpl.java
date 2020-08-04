package com.b2wdigital.fazemu.service.impl;

import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.bind.JAXBContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.service.DocumentoFiscalService;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.form.DocumentoFiscalForm;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.utils.ChaveAcessoNFe;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.resNFe_v1.ResNFe;

/**
 * Documento Fiscal Service.
 *
 * @author Marcelo Oliveira {marcelo.doliveira@b2wdigital.com}
 * @version 1.0
 */
@Service
public class DocumentoFiscalServiceImpl implements DocumentoFiscalService {

    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentoFiscalServiceImpl.class);

    @Autowired
    private MessageSource ms;
    private final Locale locale = LocaleContextHolder.getLocale();

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Override
    public List<DocumentoFiscal> listByFiltros(String tipoDocumentoFiscal,
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
            Long quantidadeRegistros) {

        return documentoFiscalRepository.listByFiltros(tipoDocumentoFiscal,
                idEmissor,
                idDestinatario,
                idEstado,
                idMunicipio,
                chaveAcesso,
                numeroDocumentoFiscal,
                numeroInicialDocumentoFiscal,
                numeroFinalDocumentoFiscal,
                serieDocumentoFiscal,
                numeroDocumentoFiscalExterno,
                tipoEmissao,
                situacaoDocumento,
                situacao,
                dataHoraRegistroInicio,
                dataHoraRegistroFim,
                quantidadeRegistros);
    }

    @Override
    public List<DocumentoFiscal> listByDateIntervalAndIdEstadoAndSituacao(String tipoDocumentoFiscal, Date dataHoraInicio, Date dataHoraFim, Long idEstado, SituacaoEnum situacao, String excludeList) {
        return documentoFiscalRepository.listByDateIntervalAndIdEstadoAndSituacao(tipoDocumentoFiscal, dataHoraInicio, dataHoraFim, idEstado, situacao, excludeList);
    }

    @Override
    public List<DocumentoFiscal> listByDateIntervalAndIdEstadoAndSituacaoAutorizacao(String tipoDocumentoFiscal, Date dataHoraInicio, Date dataHoraFim, Long idEstado, String excludeList) {
        return documentoFiscalRepository.listByDateIntervalAndIdEstadoAndSituacaoAutorizacao(tipoDocumentoFiscal, dataHoraInicio, dataHoraFim, idEstado, excludeList);
    }

    @Override
    public int updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(Long idDocFiscal, String cStat, PontoDocumentoEnum pontoDocumento, SituacaoEnum situacao, Long tipoEmissao, String situacaoDocumento) {
        return documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(idDocFiscal, cStat, pontoDocumento, situacao, tipoEmissao, situacaoDocumento);
    }

    @Override
    public String getSituacaoDocumentoByIdDocFiscal(Long idDocFiscal) {
        return documentoFiscalRepository.getSituacaoDocumentoByIdDocFiscal(idDocFiscal);
    }

    @Override
    public DocumentoFiscal findByChaveAcesso(String chaveAcesso) {
        return documentoFiscalRepository.findByChaveAcesso(chaveAcesso);
    }

    @Override
    public DocumentoFiscal findById(Long idDocFiscal) {
        return documentoFiscalRepository.findById(idDocFiscal);
    }

    @Override
    public int updateCstat(Long idDocFiscal, String cStat) {
        return documentoFiscalRepository.updateCstat(idDocFiscal, cStat);
    }

    @Override
    public int updateDataHora(Long idDocFiscal, Date dataHora) {
        return documentoFiscalRepository.updateDataHora(idDocFiscal, dataHora);
    }

    @Override
    public long insert(DocumentoFiscal documentoFiscal) {
        return documentoFiscalRepository.insert(documentoFiscal);
    }

    @Override
    public String getXMLResumoNFeByChaveAcesso(String chaveAcesso) {
        try {
            return documentoFiscalRepository.getXMLResumoNFeByChaveAcesso(chaveAcesso);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public DocumentoFiscalForm persist(DocumentoFiscalForm form) {
        List<String> validacao = new ArrayList<>();
        long idDocumentoFiscal;

        try {
            DocumentoFiscal docu = documentoFiscalRepository.findByChaveAcesso(form.getChaveAcesso());
            if (docu == null) {
                DocumentoFiscal documentoFiscal = criaDocumentoFiscal(form);
                idDocumentoFiscal = documentoFiscalRepository.insert(documentoFiscal);
                validacao.add(ms.getMessage("success.add.document", null, locale));
                form.setSuccess(true);
            } else {
                idDocumentoFiscal = docu.getId();
                validacao.add(ms.getMessage("error.documento.duplicado", null, locale));
                form.setSuccess(false);
            }
            form.setIdDocumentoFiscal(idDocumentoFiscal);
        } catch (Exception e) {
            LOGGER.error(ExceptionUtils.getStackTrace(e), e);
            validacao.add(ExceptionUtils.getRootCauseMessage(e));
            form.setSuccess(false);
        }

        form.getRetorno().addAll(validacao);
        return form;
    }

    @Override
    public DocumentoFiscal criaDocumentoFiscal(DocumentoFiscalForm form) throws Exception {
        //Verifica existencia do documento fiscal
        DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(form.getChaveAcesso());

        if (documentoFiscal == null) {

            try {

                ChaveAcessoNFe ch = ChaveAcessoNFe.unparseKey(form.getChaveAcesso());

                DocumentoFiscal df = new DocumentoFiscal();
                df.setChaveAcesso(form.getChaveAcesso());
                df.setChaveAcessoEnviada(form.getChaveAcesso());
                df.setTipoDocumentoFiscal(TIPO_DOCUMENTO_FISCAL_NFE);
                df.setIdEmissor(Long.parseLong(ch.cnpjCpf));
                df.setNumeroDocumentoFiscal(Long.parseLong(ch.nNF));
                df.setSerieDocumentoFiscal(Long.parseLong(ch.serie));
                Integer codigoIbge = Integer.valueOf(ch.cUF);
                df.setIdEstado(estadoRepository.findByCodigoIbge(codigoIbge).getId());
                df.setIdPonto(PontoDocumentoEnum.PROCESSADO.getCodigo());
                df.setSituacaoAutorizador(null);
                df.setUsuarioReg(form.getUsuario().toUpperCase());
                df.setUsuario(form.getUsuario().toUpperCase());
                df.setSituacao(SituacaoEnum.LIQUIDADO.getCodigo());
                df.setSituacaoDocumento(SituacaoDocumentoEnum.AUTORIZADO.getCodigo());
                df.setAnoDocumentoFiscal(FazemuUtils.getFullYearFormat(Integer.valueOf(ch.dataAAMM.substring(0, 2))));
                df.setIdSistema(null);
                df.setTipoEmissao(Long.parseLong(ch.tpEmis));
                df.setIdDestinatario(form.getIdDestinatario());

                return df;
            } catch (Exception e) {
                throw new Exception(ms.getMessage("error.decomposicao.chave", null, locale) + e.getMessage());
            }
        }
        return documentoFiscal;

    }

    @Override
    public List<DocumentoFiscal> listByFiltros(Map<String, String> parameters) throws Exception {
        return documentoFiscalRepository.listByFiltros(parameters);

    }

    @Override
    public TNfeProc getNotaFiscalCompleta(String chaveAcesso, JAXBContext context) {
        try {

            DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);

            String xmlProcessado = documentoClobRepository.getXmlRetornoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);

            if (xmlProcessado == null) {
                xmlProcessado = documentoClobRepository.getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);
            }

            Document docFinal = XMLUtils.convertStringToDocument(xmlProcessado);
            docFinal.setXmlStandalone(true);

            TNfeProc tNFe = (TNfeProc) context.createUnmarshaller().unmarshal(docFinal);
            return tNFe;

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ResNFe getNotaFiscalResumida(String chaveAcesso, JAXBContext context) {
        try {
            String xml = this.getXMLResumoNFeByChaveAcesso(chaveAcesso);

            if (xml != null) {
                Document docFinal = XMLUtils.convertStringToDocument(xml);
                docFinal.setXmlStandalone(true);

                ResNFe resNFe = (ResNFe) context.createUnmarshaller().unmarshal(docFinal);
                return resNFe;
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    @Override
    public List<DocumentoFiscal> listByDateIntervalAndSituacaoAndNotExistsInev(String tipoDocumentoFiscal, Date dataHoraInicio, Date dataHoraFim, SituacaoEnum situacao, String excludeList) {
        return documentoFiscalRepository.listByDateIntervalAndSituacaoAndNotExistsInev(tipoDocumentoFiscal, dataHoraInicio, dataHoraFim, situacao, excludeList);
    }

    @Override
    public List<DocumentoFiscal> listByDateIntervalAndSituacaoAndNotExistsDoev(String tipoDocumentoFiscal, Date dataHoraInicio, Date dataHoraFim, SituacaoEnum situacao, String excludeList) {
        return documentoFiscalRepository.listByDateIntervalAndSituacaoAndNotExistsDoev(tipoDocumentoFiscal, dataHoraInicio, dataHoraFim, situacao, excludeList);
    }

}
