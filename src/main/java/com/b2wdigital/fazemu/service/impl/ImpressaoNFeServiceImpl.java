package com.b2wdigital.fazemu.service.impl;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;

import javax.xml.bind.JAXBContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.xml.transform.StringSource;

import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.repository.EmissorRaizRepository;
import com.b2wdigital.fazemu.business.repository.ImpressoraRepository;
import com.b2wdigital.fazemu.business.service.DanfeService;
import com.b2wdigital.fazemu.business.service.ImpressaoNFeService;
import com.b2wdigital.fazemu.business.service.TipoEmissaoService;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.EmissorRaiz;
import com.b2wdigital.fazemu.domain.Impressora;
import com.b2wdigital.fazemu.domain.TipoEmissao;
import com.b2wdigital.fazemu.domain.form.ImpressaoNFeForm;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.exception.NotFoundException;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.fazemu.utils.XMLUtils;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.InfAdic.ObsCont;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.itextpdf.text.Document;

/**
 * ImpressaoNFe Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class ImpressaoNFeServiceImpl implements ImpressaoNFeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImpressaoNFeServiceImpl.class);

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private EmissorRaizRepository emissorRaizRepository;

    @Autowired
    private ImpressoraRepository impressoraRepository;

    @Autowired
    private DanfeService danfeService;

    @Autowired
    private TipoEmissaoService tipoEmissaoService;

    @Autowired
    private com.b2wdigital.fazemu.service.impl.PrintService printService;

    @Autowired
    @Qualifier("nfeRetAutorizacaoContext")
    private JAXBContext contextRetAutorizacao;

    @Override
    public void imprimirComNFe(ImpressaoNFeForm form) throws Exception {
        LOGGER.debug("ImpressaoNFeServiceImpl: imprimirComNFe form {}", form);

        if (StringUtils.isBlank(form.getChaveAcesso())) {
            throw new NotFoundException("Chave de Acesso nao informada");
        }
        if (StringUtils.isBlank(form.getImpressora())) {
            throw new NotFoundException("Impressora nao informada");
        }

        // Consulta via chave de acesso
        DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(form.getChaveAcesso());
        if (documentoFiscal == null) {
            throw new NotFoundException("NFe nao localizada: " + form.getChaveAcesso());
        }
        if (documentoFiscal.getSituacaoAutorizador() == null) {
            throw new NotFoundException("NFe nao processada. Situacao autorizador nao encontrada");
        }
        if (!"A".equals(documentoFiscal.getSituacaoDocumento())) {
            throw new NotFoundException("NFe não autorizada.");
        }

        // Consulta o documento retorno
        String xmlProcessado = documentoClobRepository.getXmlRetornoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);

        if (xmlProcessado == null) {
            xmlProcessado = documentoClobRepository.getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);
        }

        if (xmlProcessado == null) {
            throw new NotFoundException("NFe nao autorizada: " + form.getChaveAcesso());
        }

        //DocumentoClob clob = documentoClobRepository.findById(documentoRetorno.getIdXml());
        TNfeProc tNFeProc = (TNfeProc) contextRetAutorizacao.createUnmarshaller().unmarshal(new StringSource(xmlProcessado));

        imprimir(tNFeProc, form.getImpressora(), false);
    }

    @Override
    public void imprimirSemNFe(String nfe) throws Exception {
        LOGGER.debug("ImpressaoNFeServiceImpl: imprimirSemNFe");

        org.w3c.dom.Document docFinal = XMLUtils.convertStringToDocument(nfe);

        TNfeProc tNFeProc = null;
        try {
            tNFeProc = (TNfeProc) contextRetAutorizacao.createUnmarshaller().unmarshal(docFinal);
        } catch (Exception e) {
            LOGGER.error("Erro ao fazer o parse de nfe com protocolo", e);
        }

        if (tNFeProc == null) {
            try {
                final TNFe tNFe = (TNFe) contextRetAutorizacao.createUnmarshaller().unmarshal(docFinal);
                tNFeProc = new TNfeProc();
                tNFeProc.setNFe(tNFe);
                tNFeProc.setVersao(tNFe.getInfNFe().getVersao());
            } catch (Exception e) {
                LOGGER.error("Erro ao fazer o parse de nfe sem protocolo", e);
            }
        }

        if (tNFeProc == null) {
            throw new Exception("Estrutura de NFe fora de padrão");
        }

        String impressora = obterImpressora(tNFeProc);
        if (StringUtils.isBlank(impressora)) {
            throw new NotFoundException("Impressora nao informada");
        }

        imprimir(tNFeProc, impressora, true);
    }

    protected void imprimir(TNfeProc tNFeProc, String impressora, Boolean isNFeExterna) throws Exception {

        // Transform pdf to byte array
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Document document = danfeService.fromNfeProcToPDF(tNFeProc, os, isNFeExterna);
        if (document != null && os.size() > 0) {
            byte[] bytes = Base64.getEncoder().encode(os.toByteArray());
            IOUtils.write(bytes, os);
        } else {
            throw new NotFoundException("Nao foi possivel realizar a impressao a partir da nfe informada");
        }

        printService.print(os.toByteArray(), impressora, tNFeProc.getNFe().getInfNFe().getId());

    }

    protected String obterImpressora(TNfeProc nfeProc) {
        try {
            String impressora = null;

            List<ObsCont> obsContList = nfeProc.getNFe().getInfNFe().getInfAdic().getObsCont();
            for (ObsCont obsCont : obsContList) {
                if ("Impressora".equalsIgnoreCase(obsCont.getXCampo())) {
                    impressora = obsCont.getXTexto();
                    break; // campo de informacoes adicionais com a impressora encontrada; encerra o loop
                }
            }

            if (impressora != null) {
                return impressora;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void segundaViaImpressao(DocumentoFiscal docu, Integer tipoEmissao) {
        LOGGER.info("segundaViaImpressao docu {} tipoEmissao {}", docu.getId(), tipoEmissao);

        try {
            TipoEmissao tpEmissao = tipoEmissaoService.findByIdTipoEmissao(tipoEmissao.longValue());
            if (tpEmissao == null) {
                throw new FazemuServiceException("Tipo Emissao não encontrado por id " + tipoEmissao);
            }

            // Caso indicador de impressao esteja como SIM
            if ("S".equals(tpEmissao.getIndicadorImpressao())) {

                // Busca impressora padrão do emissor raiz
                Long raizCnpj = FazemuUtils.obterRaizCNPJ(docu.getIdEmissor());
                EmissorRaiz emissorRaiz = emissorRaizRepository.findEmissorRaizById(raizCnpj.toString());
                if (emissorRaiz == null) {
                    throw new FazemuServiceException("Emissor Raiz não encontrado por id " + raizCnpj);
                }

                if (emissorRaiz.getIdImpressora() != null) {

                    // Busca dados da impressora do emissor raiz
                    Impressora impressora = impressoraRepository.findById(emissorRaiz.getIdImpressora());
                    if (impressora == null) {
                        throw new FazemuServiceException("Impressora não encontrada por id " + emissorRaiz.getIdImpressora());
                    }

                    ImpressaoNFeForm form = ImpressaoNFeForm.build(docu.getChaveAcesso(), impressora.getNome());
                    imprimirComNFe(form);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Nao foi possivel efetuar a impressao em segunda via {} ", docu.getId(), e.getMessage());
        }
    }

}
