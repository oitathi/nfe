package com.b2wdigital.fazemu.service.impl;

import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.b2wdigital.fazemu.business.repository.CodigoRetornoAutorizadorRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoEventoRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoRetornoRepository;
import com.b2wdigital.fazemu.business.service.ProcessarRetornoCartaCorrecaoService;
import com.b2wdigital.fazemu.domain.DocumentoClob;
import com.b2wdigital.fazemu.domain.DocumentoEvento;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.enumeration.CodigoRetornoAutorizadorEnum;
import com.b2wdigital.fazemu.enumeration.PontoDocumentoEnum;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;
import com.b2wdigital.fazemu.enumeration.SituacaoEnum;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TEvento;
import com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TProcEvento;
import com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TRetEnvEvento;
import com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TretEvento;
import com.google.common.collect.Maps;

@Service
public class ProcessarRetornoCartaCorrecaoServiceImpl implements ProcessarRetornoCartaCorrecaoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessarRetornoCartaCorrecaoServiceImpl.class);

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private DocumentoEventoRepository documentoEventoRepository;

    @Autowired
    private DocumentoRetornoRepository documentoRetornoRepository;

    @Autowired
    private CodigoRetornoAutorizadorRepository codigoRetornoAutorizadorRepository;

    @Autowired
    @Qualifier("nfeRecepcaoEventoCartaCorrecaoContext")
    private JAXBContext contextCartaCorrecao;

    @Override
    public Map<DocumentoFiscal, String> processarDocumentosCartaCorrecao(Long idLote, Object content, TipoServicoEnum documentoRetorno, String usuario) throws Exception {
        LOGGER.info("processarDocumentosCartaCorrecao {} ", idLote);

        Map<DocumentoFiscal, String> mapToCallback = Maps.newHashMap();

        TRetEnvEvento tRetEnvEvento = (TRetEnvEvento) contextCartaCorrecao.createUnmarshaller().unmarshal((Node) content);

        for (TretEvento tRetEvento : ListUtils.emptyIfNull(tRetEnvEvento.getRetEvento())) {

            String chaveAcesso = tRetEvento.getInfEvento().getChNFe();
            DocumentoFiscal docu = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);
            Long idDocFiscal = docu.getId();

            String xmlProcessado = gerarXmlProcessadoCartaCorrecao(tRetEvento, idDocFiscal);

            Long idClob = documentoClobRepository.insert(DocumentoClob.build(null, xmlProcessado, usuario));

            documentoEventoRepository.insert(DocumentoEvento.build(null, idDocFiscal, PontoDocumentoEnum.PROCESSADO.getCodigo(), documentoRetorno.getTipoRetorno(), tRetEvento.getInfEvento().getCStat(), idClob, usuario));

            documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(idDocFiscal, tRetEvento.getInfEvento().getCStat(), PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, null, null);

            //Grava a documento retorno apenas quando documento foi processado com sucesso
            String situacaoAutorizacao = codigoRetornoAutorizadorRepository.getSituacaoAutorizacaoById(Integer.valueOf(tRetEvento.getInfEvento().getCStat()));
            if (StringUtils.equals(situacaoAutorizacao, CodigoRetornoAutorizadorEnum.AUTORIZADO_FINALIZADO.getCodigo())) {
                if (documentoRetornoRepository.findByIdDocFiscalAndTpServicoAndTpEvento(idDocFiscal, documentoRetorno.getTipoRetorno(), null) == null) {
                    documentoRetornoRepository.insert(idDocFiscal, documentoRetorno.getTipoRetorno(), null, idClob, usuario, usuario);
                } else {
                    documentoRetornoRepository.update(idDocFiscal, documentoRetorno.getTipoRetorno(), null, idClob, usuario);
                }
            }

            // Adiciona ao map do callback
            mapToCallback.put(docu, xmlProcessado);
        }

        LOGGER.info("Atualizado retorno do lote {} ", idLote);
        return mapToCallback;
    }

    /**
     * Gera XML com protocolo para carta de correcao
     *
     * @param tretEvento
     * @param idDocFiscal
     * @return
     * @throws JAXBException
     */
    private String gerarXmlProcessadoCartaCorrecao(TretEvento tretEvento, Long idDocFiscal) throws JAXBException {
        LOGGER.info("gerarXmlProcessadoCartaCorrecao {} {} ", tretEvento, idDocFiscal);

        StringResult stringResult = new StringResult();

        String xmlCCOR = documentoClobRepository.getXmlCartaCorrecaoEnviadoByChaveAcesso(documentoFiscalRepository.findById(idDocFiscal).getChaveAcesso());
        TEvento tEvento = (TEvento) contextCartaCorrecao.createUnmarshaller().unmarshal(new StringSource(xmlCCOR));

        TProcEvento procEventoNFe = new TProcEvento();
        procEventoNFe.setEvento(tEvento);
        procEventoNFe.setRetEvento(tretEvento);
        procEventoNFe.setVersao(ServicosEnum.RECEPCAO_EVENTO_CARTA_CORRECAO.getVersao());

        contextCartaCorrecao.createMarshaller().marshal(procEventoNFe, stringResult);

        return stringResult.toString();
    }

    @Override
    public Map<DocumentoFiscal, String> liquidarCartaCorrecao(ResumoLote lote, Object content, TipoServicoEnum documentoRetorno, String usuario) throws Exception {
        LOGGER.info("liquidarCartaCorrecao {} {} ", lote, documentoRetorno);

        Map<DocumentoFiscal, String> mapToCallback = Maps.newHashMap();

        TRetEnvEvento tRetEnvEvento = (TRetEnvEvento) contextCartaCorrecao.createUnmarshaller().unmarshal((Node) content);

        //Preenche dado que a SEFAZ não devolve em caso de falha
        DocumentoFiscal docu = documentoFiscalRepository.findById(lote.getIdDocFiscalList().get(0));

        //seta a chave no retorno para identificação no sistema cliente
        if (tRetEnvEvento.getRetEvento() == null || tRetEnvEvento.getRetEvento().isEmpty()) {
            TretEvento.InfEvento infEvento = new TretEvento.InfEvento();
            infEvento.setChNFe(docu.getChaveAcesso());

            TretEvento tRetEvento = new TretEvento();
            tRetEvento.setInfEvento(infEvento);

            tRetEnvEvento.getRetEvento().add(tRetEvento);
        } else {
            tRetEnvEvento.getRetEvento().get(0).getInfEvento().setChNFe(docu.getChaveAcesso());
        }

        Document xml = XMLUtils.createNewDocument();
        contextCartaCorrecao.createMarshaller().marshal(tRetEnvEvento, xml);

        String xmlProcessado = XMLUtils.convertDocumentToString(xml);

        Long idClob = documentoClobRepository.insert(DocumentoClob.build(null, xmlProcessado, usuario));

        documentoEventoRepository.insert(DocumentoEvento.build(null, lote.getIdDocFiscalList().get(0), PontoDocumentoEnum.PROCESSADO.getCodigo(), null, null, idClob, usuario));

        documentoFiscalRepository.updatePontoAndSituacaoAndCstatAndTipoEmissaoAndSituacaoDocumento(lote.getIdDocFiscalList().get(0), null, PontoDocumentoEnum.PROCESSADO, SituacaoEnum.LIQUIDADO, null, null);

        // Adiciona ao map do callback
        mapToCallback.put(docu, xmlProcessado);

        return mapToCallback;
    }

}
