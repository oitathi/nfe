package com.b2wdigital.fazemu.service.impl;

import javax.xml.bind.JAXBContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.service.ConsultarRetornoService;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.domain.form.ConsultarRetornoForm;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.NotFoundException;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.b2wdigital.nfe.schema.v4v160b.inutNFe_v4.TRetInutNFe;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author dailton.almeida
 */
@Service
public class ConsultarRetornoServiceImpl implements ConsultarRetornoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarRetornoServiceImpl.class);

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    @Qualifier("nfeRetAutorizacaoContext")
    private JAXBContext contextRetAutorizacao;

    @Autowired
    @Qualifier("nfeRecepcaoEventoCancelamentoContext")
    private JAXBContext contextCancelamento;

    @Autowired
    @Qualifier("nfeRecepcaoEventoCartaCorrecaoContext")
    private JAXBContext contextCartaCorrecao;

    @Autowired
    @Qualifier("nfeRecepcaoEventoManifestacaoContext")
    private JAXBContext contextManifestacao;

    @Autowired
    @Qualifier("nfeInutilizacaoContext")
    private JAXBContext contextInutilizacao;

    @Override
    public String consultarRetornoAsString(ConsultarRetornoForm form) {
        LOGGER.debug("ConsultarRetornoServiceImpl: consultarRetornoAsString form {}", form);

        // Determina documento fiscal
        DocumentoFiscal documentoFiscal = getDocumentoFiscal(form);

        // Determina tipo de retorno
        String tipoRetorno = getTipoRetorno(form);

        String xmlProcessado = documentoClobRepository.getXmlRetornoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.getByTipoRetorno(tipoRetorno));

        if (xmlProcessado == null) {
            xmlProcessado = documentoClobRepository.getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.getByTipoRetorno(tipoRetorno));
        }

        if (xmlProcessado == null) {
            throw new NotFoundException("Retorno nao encontrado: " + TipoServicoEnum.getByTipoRetorno(tipoRetorno));
        }

        return xmlProcessado;

    }

    protected DocumentoFiscal getDocumentoFiscal(ConsultarRetornoForm form) {

        // Consulta via chave de acesso
        if (!StringUtils.isBlank(form.getChaveAcesso())) {
            DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(form.getChaveAcesso());

            if (documentoFiscal == null) {
                throw new NotFoundException("NFe nao localizada: " + form.getChaveAcesso());
            }

            return documentoFiscal;

            // Consulta via documento fiscal
        } else if (form.getIdEmissor() != null && form.getNumeroDocumentoFiscal() != null
                && form.getSerieDocumentoFiscal() != null && form.getAnoDocumentoFiscal() != null && form.getCodigoIBGE() != null) {

            Estado estado = estadoRepository.findByCodigoIbge(form.getCodigoIBGE());

            DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByDadosDocumentoFiscal(form.getTipoDocumentoFiscal(), form.getIdEmissor(), form.getNumeroDocumentoFiscal(), form.getSerieDocumentoFiscal(), form.getAnoDocumentoFiscal(), estado.getId());
            if (documentoFiscal == null) {
                throw new NotFoundException("NFe nao localizada com dados: " + form);
            }

            return documentoFiscal;
        }

        throw new NotFoundException("Dados insuficientes para busca: " + form);
    }

    protected String getTipoRetorno(ConsultarRetornoForm form) {

        String tipoRetorno = StringUtils.isBlank(form.getTipoRetorno())
                ? TipoServicoEnum.AUTORIZACAO.getTipoRetorno()
                : form.getTipoRetorno();

        if (TipoServicoEnum.getByTipoRetorno(tipoRetorno) == null) {
            throw new NotFoundException("Tipo Retorno nao conhecido: " + tipoRetorno);
        }

        return tipoRetorno;
    }

    @Override
    public Object consultarRetornoAsObject(ConsultarRetornoForm form) throws Exception {
        LOGGER.debug("ConsultarRetornoServiceImpl: consultarRetornoAsObject form {}", form);

        String retorno = consultarRetornoAsString(form);

        //Converte para Document
        Document document = XMLUtils.convertStringToDocument(retorno);
        XMLUtils.cleanNameSpace(document);
        document.setXmlStandalone(true);

        String tipoRetorno = getTipoRetorno(form);

        ObjectMapper objectMapper = new ObjectMapper();
        if (TipoServicoEnum.AUTORIZACAO.getTipoRetorno().equals(tipoRetorno)) {
            TNfeProc tNfeProc = (TNfeProc) contextRetAutorizacao.createUnmarshaller().unmarshal(document);
            return objectMapper.writeValueAsString(tNfeProc);
        } else if (TipoServicoEnum.CANCELAMENTO.getTipoRetorno().equals(tipoRetorno)) {
            com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TRetEvento tRetEvento
                    = (com.b2wdigital.nfe.schema.v4v160b.cancel.envEventoCancNFe_v1.TRetEvento) contextCancelamento.createUnmarshaller().unmarshal(document);
            return objectMapper.writeValueAsString(tRetEvento);
        } else if (TipoServicoEnum.CARTA_CORRECAO.getTipoRetorno().equals(tipoRetorno)) {
            com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TretEvento tRetEvento
                    = (com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TretEvento) contextCartaCorrecao.createUnmarshaller().unmarshal(document);
            return objectMapper.writeValueAsString(tRetEvento);
        } else if (TipoServicoEnum.MANIFESTACAO.getTipoRetorno().equals(tipoRetorno)) {
            com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TretEvento tRetEvento
                    = (com.b2wdigital.nfe.schema.v4v160b.manifestacao.envConfRecebto_v1.TretEvento) contextManifestacao.createUnmarshaller().unmarshal(document);
            return objectMapper.writeValueAsString(tRetEvento);
        } else if (TipoServicoEnum.INUTILIZACAO.getTipoRetorno().equals(tipoRetorno)) {
            TRetInutNFe tRetInutNFe = (TRetInutNFe) contextInutilizacao.createUnmarshaller().unmarshal(document);
            return objectMapper.writeValueAsString(tRetInutNFe);
        }

        throw new NotFoundException("Erro ao obter retorno em formato JSON");

    }

}
