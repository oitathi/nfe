package com.b2wdigital.fazemu.service.pdf;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.xml.transform.StringSource;

import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.DacceService;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.exception.NotFoundException;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TEvento;
import com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TProcEvento;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TEnderEmi;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TEndereco;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.Dest;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.Emit;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class DacceServiceImpl extends AbstractPdfService implements DacceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DacceServiceImpl.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();

    //BLOCO 1
    private static final String LABEL_IDENTIFICACAO_EMITENTE = "IDENTIFICA\u00C7\u00C3O DO EMITENTE";

    private static final String LABEL_DACCE_SIGLA = "DACCE";
    private static final String LABEL_DACCE_DESCRICAO = "DOCUMENTO AUXILIAR DA CARTA DE CORREÇÃO ELETRÔNICA";
    private static final String LABEL_NNF = "No. ";
    private static final String LABEL_SERIE = "S\u00E9rie ";
    private static final String LABEL_SEQ = "SEQ.";

    private static final String LABEL_PROTOCOLO_AUTORIZACAO_USO = "PROTOCOLO DE AUTORIZA\u00C7\u00C3O DE USO";
    private static final String LABEL_DATAHORA_REGISTRO_EVENTO = "DATA/HORA DO REGISTRO DO EVENTO";
    private static final String LABEL_CHAVE_ACESSO = "CHAVE DE ACESSO";

    private static final String LABEL_CNPJ = "CNPJ";
    private static final String LABEL_INSCRICAO_ESTADUAL = "INSCRI\u00C7\u00C3O ESTADUAL";

    //BLOCO 2
    private static final String LABEL_DESTINATARIO = "DESTINAT\u00C1RIO";
    private static final String LABEL_NOME_RAZAO_SOCIAL = "NOME / RAZ\u00C3O SOCIAL";
    private static final String LABEL_CNPJ_CPF_IDESTRANGEIRO = "CNPJ/CPF/ID Estrangeiro";
    private static final String LABEL_DT_EMISSAO_NFE = "DATA DA EMISS\u00C3O DA NF-E";
    private static final String LABEL_ENDERECO = "ENDERE\u00C7O";
    private static final String LABEL_BAIRRO_DISTRITO = "BAIRRO/DISTRITO";
    private static final String LABEL_CEP = "CEP";
    private static final String LABEL_MUNICIPIO = "MUNIC\u00CDPIO";
    private static final String LABEL_FONE_FAX = "FONE / FAX";
    private static final String LABEL_UF = "UF";

    // BLOCO 3
    private static final String LABEL_CORRECAO = "CORREÇÃO A SER CONSIDERADA";

    // BLOCO 4
    private static final String LABEL_CONDICAO_USO = "CONDIÇÃO DE USO";

    private static final String OUTPUT_DATE_FORMAT = "dd/MM/yyyy";
    private static final String OUTPUT_DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    private static final Font DACCE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD);
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD);
    private static final Font DEFAULT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8);

    private static final float MARGIN_LEFT = 13f, MARGIN_RIGHT = 14f, MARGIN_TOP = 20f, MARGIN_BOTTOM = 30f;
    private static final float FOOTER_TITLE_HEIGHT = 10f;
    private static final float FOOTER_HEIGHT = 65f;

    @Autowired
    @Qualifier("nfeRetAutorizacaoContext")
    private JAXBContext context;
    @Autowired
    @Qualifier("nfeRecepcaoEventoCartaCorrecaoContext")
    private JAXBContext contextCartaCorrecao;
    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;
    @Autowired
    private DocumentoClobRepository documentoClobRepository;
    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Override
    public Document fromChaveAcessoToPDF(String chaveAcesso, OutputStream outputStream) throws JAXBException {
        chaveAcesso = FazemuUtils.normalizarChaveAcesso(chaveAcesso);

        // Consulta via chave de acesso
        DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);
        if (documentoFiscal == null) {
            throw new NotFoundException("NFe nao localizada: " + chaveAcesso);
        }

        if (!"A".equals(documentoFiscal.getSituacaoDocumento())) {
            throw new NotFoundException("NFe não autorizada.");
        }
        // documento fiscal processado
        String xmlProcessado = documentoClobRepository.getXmlRetornoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);

        if (xmlProcessado == null) {
            xmlProcessado = documentoClobRepository.getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);
        }

        if (xmlProcessado == null) {
            throw new FazemuServiceException("Documento Fiscal não encontrado para a chave de acesso: " + chaveAcesso);
        }

        // Carta de correção processada
        String xmlDacce = documentoClobRepository.getXmlRetornoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.CARTA_CORRECAO);
        if (xmlDacce == null) {
            throw new FazemuServiceException("Carta de correção não encontrada para a chave de acesso: " + chaveAcesso);
        }

        TNfeProc nfeProc = (TNfeProc) context.createUnmarshaller().unmarshal(new StringSource(xmlProcessado));
        TProcEvento tProcEvento = (TProcEvento) contextCartaCorrecao.createUnmarshaller().unmarshal(new StringSource(xmlDacce));

        return this.fromDacceToPDF(nfeProc, tProcEvento, outputStream);
    }

    @Override
    public Document fromDacceToPDF(TNfeProc nfeProc, TProcEvento tProcEvento, OutputStream outputStream) {
        try {
            Document document = new Document(PageSize.A4, MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP, MARGIN_BOTTOM + FOOTER_TITLE_HEIGHT + FOOTER_HEIGHT);
            PdfWriter pdfWriter = PdfWriter.getInstance(document, outputStream);
            pdfWriter.setPageEvent(new HeaderFooterDaccePageEvent(this, tProcEvento, nfeProc));
            document.open(); //precisa abrir depois da criacao do PdfWriter

            document.addTitle(tProcEvento.getEvento().getInfEvento().getId());
//          cabecalho(pdfWriter, document, nfeProc); // ver HeaderFooterDaccePageEvent

            spaceTable(document);

            secaoDestinatario(document, nfeProc.getNFe());

            spaceTable(document);

            secaoCorrecao(document, tProcEvento.getEvento());

            spaceTable(document);

            secaoCondicaoUso(document, tProcEvento.getEvento());

            document.close();
            return document;

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new FazemuServiceException(e.getMessage());
        }
    }

    /**
     * Tabela que serve para dar espaco no documento
     *
     * @param document
     * @throws DocumentException
     */
    protected void spaceTable(Document document) throws DocumentException {
        PdfPTable spaceTable = buildTable(new int[]{1});
        PdfPCell c = addCell(spaceTable, StringUtils.SPACE, SECTION_FONT, Paragraph.ALIGN_LEFT);
        c.setBorder(PdfPCell.NO_BORDER);
        document.add(spaceTable);
    }

    protected void cabecalho(PdfWriter pdfWriter, Document document, TProcEvento tProcEvento, TNfeProc nfeProc) throws Exception {
        PdfPTable table;

        TNFe nfe = nfeProc.getNFe();
        TEvento evento = tProcEvento.getEvento();

        table = buildTable(new int[]{7, 3, 8});
        table.addCell(celulaIdentificacaoDoEmitente(nfe));
        table.addCell(celulaDACCE(pdfWriter, nfe, evento));

        String numeroProtocolo = tProcEvento.getRetEvento().getInfEvento().getNProt();
        table.addCell(celulaProtocoloAutorizacaoUso(numeroProtocolo));

        table.addCell(celulaDataHoraRegistroEvento(evento));

        this.celulaChaveDeAcesso(nfe, table);

        //second row
        Emit emitente = nfe.getInfNFe().getEmit();
        addCell(table, LABEL_CNPJ, TITLE_FONT, cnvl(FazemuUtils.maskCNPJorCPF(emitente.getCNPJ(), emitente.getCPF())), DEFAULT_FONT, Paragraph.ALIGN_LEFT, 2);
        addCell(table, LABEL_INSCRICAO_ESTADUAL, TITLE_FONT, cnvl(emitente.getIE()), DEFAULT_FONT, Paragraph.ALIGN_LEFT, 1);

        document.add(table);

    }

    protected PdfPCell celulaIdentificacaoDoEmitente(TNFe nfe) throws Exception {
        Emit emitente = nfe.getInfNFe().getEmit();
        TEnderEmi end = emitente.getEnderEmit();
        Paragraph p;
        PdfPCell cell = new PdfPCell();
        cell.setRowspan(3);

        Image logo = this.obterLogoNFe();

        if (logo == null) {
            p = centeredParagraph(LABEL_IDENTIFICACAO_EMITENTE, TITLE_FONT);
            p.setSpacingAfter(10f);
            cell.addElement(p);
        } else {
            logo.setScaleToFitHeight(false);
            logo.setScaleToFitLineWhenOverflow(false);
            logo.scaleToFit(65f, 65f);
            cell.addElement(logo);
        }

        //Nome Emitente
        cell.addElement(centeredParagraph(emitente.getXNome(), TITLE_FONT));
        cell.addElement(centeredParagraph(StringUtils.joinWith(StringUtils.SPACE, snvl(end.getXLgr()), snvl(end.getNro()), snvl(end.getXCpl())), DEFAULT_FONT));
        cell.addElement(centeredParagraph(StringUtils.joinWith(" - ", snvl(end.getXBairro()), snvl(end.getCEP()), snvl(end.getXMun()), end.getUF().value()), DEFAULT_FONT));
        return cell;
    }

    protected PdfPCell celulaDACCE(PdfWriter pdfWriter, TNFe nfe, TEvento evento) throws DocumentException {
        Paragraph p;
        PdfPCell cell = new PdfPCell();
        cell.setRowspan(3);
        cell.addElement(centeredParagraph(LABEL_DACCE_SIGLA, DACCE_FONT));
        p = centeredParagraph(LABEL_DACCE_DESCRICAO, TITLE_FONT);
        p.setSpacingAfter(4f);
        cell.addElement(p);

        p = centeredParagraph(LABEL_NNF + FazemuUtils.maskNFeNumber(nfe.getInfNFe().getIde().getNNF()), DEFAULT_FONT);
        p.setSpacingBefore(4f);
        cell.addElement(p);

        p = centeredParagraph(LABEL_SERIE + nfe.getInfNFe().getIde().getSerie(), DEFAULT_FONT);
        p.setSpacingBefore(4f);
        cell.addElement(p);

        p = centeredParagraph(LABEL_SEQ + evento.getInfEvento().getNSeqEvento(), DEFAULT_FONT);
        p.setSpacingBefore(4f);
        cell.addElement(p);

        return cell;
    }

    protected PdfPCell celulaProtocoloAutorizacaoUso(String protocoloAutorizacaoUso) {
        PdfPCell cell = new PdfPCell();
        cell.addElement(defaultParagraph(LABEL_PROTOCOLO_AUTORIZACAO_USO, TITLE_FONT));
        cell.addElement(defaultParagraph(protocoloAutorizacaoUso, DEFAULT_FONT));
        return cell;
    }

    protected PdfPCell celulaDataHoraRegistroEvento(TEvento evento) {
        PdfPCell cell = new PdfPCell();
        cell.addElement(defaultParagraph(LABEL_DATAHORA_REGISTRO_EVENTO, TITLE_FONT));
        cell.addElement(defaultParagraph(formatDateTime(evento.getInfEvento().getDhEvento(), OUTPUT_DATETIME_FORMAT), DEFAULT_FONT));
        return cell;
    }

    protected PdfPCell celulaChaveDeAcesso(TNFe nfe, PdfPTable table) {
        String chaveAcesso = nfe.getInfNFe().getId();
        int index = chaveAcesso.startsWith(FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE) ? FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.length() : 0;
        int blockSize = 4, len = chaveAcesso.length();
        StringBuilder sb = new StringBuilder(64);
        for (; index < chaveAcesso.length(); index += blockSize) {
            sb.append(StringUtils.SPACE).append(chaveAcesso.substring(index, Math.min(index + blockSize, len)));
        }
        return addCell(table, LABEL_CHAVE_ACESSO, TITLE_FONT, sb.substring(1), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
    }

    protected void secaoDestinatario(Document document, TNFe nfe) throws DocumentException {
        PdfPTable table;

        Dest dest = nfe.getInfNFe().getDest();
        TEndereco enderDest = dest.getEnderDest();
        String enderecoNumeroComplemento = cnvl(StringUtils.joinWith(StringUtils.SPACE, snvl(enderDest.getXLgr()), snvl(enderDest.getNro()), snvl(enderDest.getXCpl())));

        //linha titulo
        table = buildTable(new int[]{1});
        PdfPCell c = addCell(table, LABEL_DESTINATARIO, SECTION_FONT, Paragraph.ALIGN_LEFT);
        c.setBorder(PdfPCell.NO_BORDER);
        document.add(table);

        table = buildTable(new int[]{12, 5, 5});
        addCell(table, LABEL_NOME_RAZAO_SOCIAL, TITLE_FONT, cnvl(dest.getXNome()), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_CNPJ_CPF_IDESTRANGEIRO, TITLE_FONT, cnvl(FazemuUtils.maskCNPJorCPF(dest.getCNPJ(), dest.getCPF())), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_DT_EMISSAO_NFE, TITLE_FONT, cnvl(formatDateTime(nfe.getInfNFe().getIde().getDhEmi(), OUTPUT_DATE_FORMAT)), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        document.add(table);

        table = buildTable(new int[]{9, 4, 3});
        addCell(table, LABEL_ENDERECO, TITLE_FONT, enderecoNumeroComplemento, DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_BAIRRO_DISTRITO, TITLE_FONT, cnvl(enderDest.getXBairro()), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        String cep = enderDest.getCEP() != null && !"".equals(enderDest.getCEP()) ? FazemuUtils.formatString(StringUtils.leftPad(enderDest.getCEP(), 8, "0"), FazemuUtils.MASCARA_CEP) : "";
        addCell(table, LABEL_CEP, TITLE_FONT, cnvl(cep), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        document.add(table);

        table = buildTable(new int[]{9, 3, 1, 3});
        addCell(table, LABEL_MUNICIPIO, TITLE_FONT, cnvl(enderDest.getXMun()), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_FONE_FAX, TITLE_FONT, cnvl(enderDest.getFone()), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_UF, TITLE_FONT, cnvl(enderDest.getUF().value()), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_INSCRICAO_ESTADUAL, TITLE_FONT, cnvl(dest.getIE()), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        document.add(table);
    }

    protected void secaoCorrecao(Document document, TEvento evento) throws DocumentException {
        PdfPTable table;

        //linha titulo
        table = buildTable(new int[]{1});
        PdfPCell c = addCell(table, LABEL_CORRECAO, SECTION_FONT, Paragraph.ALIGN_LEFT);
        c.setBorder(PdfPCell.NO_BORDER);
        document.add(table);

        table = buildTable(new int[]{1});
        addCell(table, evento.getInfEvento().getDetEvento().getXCorrecao(), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        document.add(table);
    }

    protected void secaoCondicaoUso(Document document, TEvento evento) throws DocumentException {
        PdfPTable table;

        //linha titulo
        table = buildTable(new int[]{1});
        PdfPCell c = addCell(table, LABEL_CONDICAO_USO, SECTION_FONT, Paragraph.ALIGN_LEFT);
        c.setBorder(PdfPCell.NO_BORDER);
        document.add(table);

        table = buildTable(new int[]{1});
        addCell(table, evento.getInfEvento().getDetEvento().getXCondUso(), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        document.add(table);
    }

    protected Image obterLogoNFe() {
        try {
            byte[] logoNFe = parametrosInfraRepository.getAsByteArray(TIPO_DOCUMENTO_FISCAL_NFE, "NFE_LOGO", null);
            if (logoNFe != null) {
                return Image.getInstance(logoNFe);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (BadElementException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

}
