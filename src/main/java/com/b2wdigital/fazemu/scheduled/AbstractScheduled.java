package com.b2wdigital.fazemu.scheduled;

import com.b2wdigital.assinatura_digital.CertificadoDigital;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.el.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

import com.b2wdigital.fazemu.business.repository.CodigoRetornoAutorizadorRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.business.service.ConsultarProtocoloService;
import com.b2wdigital.fazemu.business.service.DocumentoClobService;
import com.b2wdigital.fazemu.business.service.DocumentoEventoService;
import com.b2wdigital.fazemu.business.service.DocumentoFiscalService;
import com.b2wdigital.fazemu.business.service.DocumentoRetornoService;
import com.b2wdigital.fazemu.business.service.EmitirLoteService;
import com.b2wdigital.fazemu.business.service.KafkaProducerService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.ResumoDocumentoFiscal;
import com.b2wdigital.fazemu.enumeration.CodigoRetornoAutorizadorEnum;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;
import com.b2wdigital.fazemu.enumeration.TipoEmissaoEnum;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TProtNFe;
import com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TRetConsSitNFe;
import org.w3c.dom.Document;

/**
 * Processos em comum
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
public abstract class AbstractScheduled {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractScheduled.class);

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private CodigoRetornoAutorizadorRepository codigoRetornoAutorizadorRepository;

    @Autowired
    private EmitirLoteService emitirLoteService;

    @Autowired
    private DocumentoFiscalService documentoFiscalService;

    @Autowired
    private DocumentoClobService documentoClobService;

    @Autowired
    private DocumentoEventoService documentoEventoService;

    @Autowired
    private DocumentoRetornoService documentoRetornoService;

    @Autowired
    private ConsultarProtocoloService consultarProtocoloService;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private CertificadoDigitalService certificadoDigitalService;

    @Autowired
    @Qualifier("nfeRetAutorizacaoContext")
    private JAXBContext context;

    @Autowired
    @Qualifier("nfeConsultarProtocoloContext")
    private JAXBContext contextConsultaProtocolo;

    protected TRetConsSitNFe consultarProtocolo(DocumentoFiscal docu) {
        try {
            LOGGER.info("consultarProtocolo {} ", docu.getId());
            return consultarProtocoloService.consultarProtocolo(docu.getChaveAcessoEnviada());
        } catch (Exception e) {
            LOGGER.error("Erro ao consultar o protocolo para chave {} ", docu.getChaveAcessoEnviada(), e);
        }
        return null;
    }

    protected void emitirNovoLote(TRetConsSitNFe tRetConsSitNFe, DocumentoFiscal docu, Integer tipoEmissaoAtual, ServicosEnum servico, DocumentoClob docClob) throws ParseException {
        LOGGER.info("emitirNovoLote {} ", docu.getId());

        if (docClob != null) {
            emitirLoteService.emitirLote(ResumoDocumentoFiscal.build(
                    docu.getId(),
                    docu.getTipoDocumentoFiscal(),
                    docu.getIdEmissor(),
                    Integer.valueOf(tRetConsSitNFe.getCUF()),
                    docu.getIdMunicipio(),
                    tipoEmissaoAtual,
                    servico.getVersao(),
                    docClob.getId(),
                    docClob.getClob().length()),
                    servico, false);
        }
    }

    protected void processarProtocoloAutorizacao(TRetConsSitNFe tRetConsSitNFe, DocumentoFiscal documentoFiscal) throws Exception {
        LOGGER.info("processarProtocoloAutorizacao {} ", documentoFiscal.getId());

        String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

        DocumentoClob documentoClob = documentoClobService.getLastXmlSignedByIdDocFiscal(documentoFiscal.getId());

        if (documentoClob == null) {
            documentoClob = assinarDocumento(documentoFiscal);
        }

        String xmlProcessado = this.encaixarProtocoloAutorizacao(documentoClob.getClob(), tRetConsSitNFe.getProtNFe(), documentoFiscal, usuario);

        kafkaProducerService.invokeCallback(documentoFiscal, xmlProcessado, TipoServicoEnum.AUTORIZACAO);
    }

    private String encaixarProtocoloAutorizacao(String xmlNFeSigned, TProtNFe protNFe, DocumentoFiscal docu, String usuario) throws JAXBException {
        LOGGER.info("encaixarProtocoloAutorizacao {} ", docu.getId());

        TNFe tNFe = (TNFe) context.createUnmarshaller().unmarshal(new StringSource(xmlNFeSigned));

        TNfeProc nfeProc = new TNfeProc();
        nfeProc.setNFe(tNFe);
        nfeProc.setProtNFe(createProtocolo(protNFe));
        nfeProc.setVersao(protNFe.getVersao());

        StringResult stringResult = new StringResult();
        context.createMarshaller().marshal(nfeProc, stringResult);
        String result = stringResult.toString();

        Long idClob = documentoClobService.insert(DocumentoClob.build(null, result, usuario));
        documentoEventoService.insert(DocumentoEvento.build(null, docu.getId(), PontoDocumentoEnum.PROCESSADO.getCodigo(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), protNFe.getInfProt().getCStat(), idClob, usuario));

        String situacaoDocumento = docu.getSituacaoDocumento();
        String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(Integer.valueOf(protNFe.getInfProt().getCStat()));
        if ("110".equals(protNFe.getInfProt().getCStat())
                || "301".equals(protNFe.getInfProt().getCStat())
                || "302".equals(protNFe.getInfProt().getCStat())
                || "303".equals(protNFe.getInfProt().getCStat())) {
            situacaoDocumento = SituacaoDocumentoEnum.DENEGADO.getCodigo();
        } else if ((SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                || SituacaoDocumentoEnum.REJEITADO.getCodigo().equals(situacaoDocumento))
                && CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
            situacaoDocumento = SituacaoDocumentoEnum.AUTORIZADO.getCodigo();
        } else if ((SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                || (SituacaoDocumentoEnum.AUTORIZADO.getCodigo().equals(situacaoDocumento)
                && TipoEmissaoEnum.EPEC.getCodigo().toString().equals(nfeProc.getNFe().getInfNFe().getIde().getTpEmis())
                && !"468".equals(protNFe.getInfProt().getCStat())))
                && !CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
            situacaoDocumento = SituacaoDocumentoEnum.REJEITADO.getCodigo();
        }

        Long tipoEmissao = docu.getTipoEmissao();
        if (tipoEmissao == null || tipoEmissao == 0) {
            tipoEmissao = Long.valueOf(tNFe.getInfNFe().getIde().getTpEmis());
        }

        documentoFiscalService.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(docu.getId(), protNFe.getInfProt().getCStat(), PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, tipoEmissao, situacaoDocumento);

        //Grava a documento retorno apenas quando documento foi processado com sucesso
        if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())) {
            if (documentoRetornoService.findByIdDocFiscalAndTpServicoAndTpEvento(docu.getId(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null) == null) {
                documentoRetornoService.insert(docu.getId(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null, idClob, usuario, usuario);
                LOGGER.info("Criou documento retorno");
            } else {
                documentoRetornoService.update(docu.getId(), TipoServicoEnum.AUTORIZACAO.getTipoRetorno(), null, idClob, usuario);
                LOGGER.info("Atualizou documento retorno");
            }
        }

        return result;
    }

    private com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe createProtocolo(TProtNFe protNFe) {

        com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe.InfProt infProt = new com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe.InfProt();
        infProt.setTpAmb(protNFe.getInfProt().getTpAmb());
        infProt.setVerAplic(protNFe.getInfProt().getVerAplic());
        infProt.setChNFe(protNFe.getInfProt().getChNFe());
        infProt.setDhRecbto(protNFe.getInfProt().getDhRecbto().toString());
        infProt.setNProt(protNFe.getInfProt().getNProt());
        infProt.setDigVal(protNFe.getInfProt().getDigVal());
        infProt.setCStat(protNFe.getInfProt().getCStat());
        infProt.setXMotivo(protNFe.getInfProt().getXMotivo());

        com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe tProtNFe = new com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TProtNFe();
        tProtNFe.setInfProt(infProt);

        return tProtNFe;
    }

    protected void processarDocumentosRecepcaoEvento(TRetConsSitNFe tRetConsSitNFe, DocumentoFiscal docu, TipoServicoEnum tipoServico) throws Exception {
        LOGGER.info("processarDocumentosRecepcaoEvento {} ", docu.getId());

        String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

        com.b2wdigital.nfe.schema.v4v160b.consSitNFe_v4.TRetEvento tRetEvento = tRetConsSitNFe.getProcEventoNFe().iterator().next().getRetEvento();

        StringResult xmlProcessado = new StringResult();
        contextConsultaProtocolo.createMarshaller().marshal(tRetEvento, xmlProcessado);

        Long idClob = documentoClobService.insert(DocumentoClob.build(null, xmlProcessado.toString(), usuario));

        documentoEventoService.insert(DocumentoEvento.build(null, docu.getId(), PontoDocumentoEnum.PROCESSADO.getCodigo(), tipoServico.getTipoRetorno(), tRetEvento.getInfEvento().getCStat(), idClob, usuario));

        String situacaoDocumento = docu.getSituacaoDocumento();
        String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(Integer.valueOf(tRetEvento.getInfEvento().getCStat()));

        if ((SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                || SituacaoDocumentoEnum.REJEITADO.getCodigo().equals(situacaoDocumento))
                && CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
            if (TipoServicoEnum.AUTORIZACAO.getTipoRetorno().equals(tipoServico.getTipoRetorno())) {
                situacaoDocumento = SituacaoDocumentoEnum.AUTORIZADO.getCodigo();
            } else if (TipoServicoEnum.CANCELAMENTO.getTipoRetorno().equals(tipoServico.getTipoRetorno())) {
                situacaoDocumento = SituacaoDocumentoEnum.CANCELADO.getCodigo();
            }
        } else if (SituacaoDocumentoEnum.AUTORIZADO.getCodigo().equals(situacaoDocumento)
                && CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
            if (TipoServicoEnum.CANCELAMENTO.getTipoRetorno().equals(tipoServico.getTipoRetorno())) {
                situacaoDocumento = SituacaoDocumentoEnum.CANCELADO.getCodigo();
            }
        } else if (SituacaoDocumentoEnum.ENVIADO.getCodigo().equals(situacaoDocumento)
                && !CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo().equals(situacaoAutorizacao)) {
            situacaoDocumento = SituacaoDocumentoEnum.REJEITADO.getCodigo();
        }

        documentoFiscalService.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(docu.getId(), tRetEvento.getInfEvento().getCStat(), PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, null, situacaoDocumento);

        //Grava a documento retorno apenas quando documento foi processado com sucesso
        if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())) {
            Long tipoEvento = null;
            if (TipoServicoEnum.MANIFESTACAO.getTipoRetorno().equals(tipoServico.getTipoRetorno())) {
                tipoEvento = Long.valueOf(tRetEvento.getInfEvento().getTpEvento());
            }

            if (documentoRetornoService.findByIdDocFiscalAndTpServicoAndTpEvento(docu.getId(), tipoServico.getTipoRetorno(), tipoEvento) == null) {
                documentoRetornoService.insert(docu.getId(), tipoServico.getTipoRetorno(), tipoEvento, idClob, usuario, usuario);
            } else {
                documentoRetornoService.update(docu.getId(), tipoServico.getTipoRetorno(), tipoEvento, idClob, usuario);
            }
        }

        kafkaProducerService.invokeCallback(docu, xmlProcessado.toString(), tipoServico);
    }

    protected DocumentoClob assinarDocumento(DocumentoFiscal documentoFiscal) {
        try {
            LOGGER.info("emitirLoteDocumentosAbertosParados (scheduled) - assinarDocumento {}", documentoFiscal.getId());

            String usuario = parametrosInfraRepository.getAsString(null, ParametrosInfraRepository.PAIN_USUARIO_DEFAULT);

            DocumentoClob documentoClob = documentoClobService.getLastXmlByIdDocFiscalAndPontoDocumento(documentoFiscal.getId(), PontoDocumentoEnum.RECEBIDO_XML_RAW);

            Document docNFe = XMLUtils.convertStringToDocument(documentoClob.getClob());

            // Certificado
            long raizCnpjEmitente = FazemuUtils.obterRaizCNPJ(Long.valueOf(documentoFiscal.getIdEmissor()));
            CertificadoDigital certificado = certificadoDigitalService.getByIdEmissorRaiz(raizCnpjEmitente);

            FazemuUtils.signXml(docNFe, certificado, ServicosEnum.AUTORIZACAO_NFE);

            Long idDocumentoXmlSignedClob = documentoClobService.insert(DocumentoClob.build(null, XMLUtils.convertDocumentToString(docNFe), usuario));
            documentoEventoService.insert(DocumentoEvento.build(null, documentoFiscal.getId(), PontoDocumentoEnum.RECEBIDO_XML_SIGNED.getCodigo(), null, null, idDocumentoXmlSignedClob, usuario));

            return documentoClobService.findById(idDocumentoXmlSignedClob);
        } catch (Exception e) {
            return null;
        }

    }

}
