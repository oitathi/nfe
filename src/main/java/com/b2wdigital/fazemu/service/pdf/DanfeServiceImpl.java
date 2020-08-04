package com.b2wdigital.fazemu.service.pdf;

import com.b2wdigital.fazemu.business.repository.DocumentoClobRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.xml.transform.StringSource;

import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.business.repository.EmissorRaizLogoRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.DanfeService;
import com.b2wdigital.fazemu.business.service.StorageService;
import com.b2wdigital.fazemu.domain.DocumentoFiscal;
import com.b2wdigital.fazemu.domain.EmissorRaizLogo;
import com.b2wdigital.fazemu.domain.form.DanfeDetalheForm;
import com.b2wdigital.fazemu.enumeration.TipoEmissaoEnum;
import com.b2wdigital.fazemu.enumeration.TipoServicoEnum;
import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.utils.DateUtils;
import com.b2wdigital.fazemu.utils.FazemuUtils;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TEnderEmi;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TEndereco;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.Dest;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.Det;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.Det.Imposto.ICMS;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.Det.Prod;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.Emit;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.InfAdic;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.InfAdic.ObsCont;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.Total.ICMSTot;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.Transp;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.Transp.Transporta;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNFe.InfNFe.Transp.Vol;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TVeiculo;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.DottedLineSeparator;

@Service
public class DanfeServiceImpl extends AbstractPdfService implements DanfeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DanfeServiceImpl.class);
    private static final String TIPO_DOCUMENTO_FISCAL_NFE = FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.toUpperCase();
    //
    private static final String LABEL_NFE = "NF-e";
    private static final String LABEL_NNF = "No. ";
    private static final String LABEL_SERIE = "S\u00E9rie ";
    private static final String LABEL_FOLHA = "Folha ";
    private static final String LABEL_DT_RECEBIMENTO = "DATA DE RECEBIMENTO";
    private static final String LABEL_IDENTIF_RECEBEDOR = "IDENTIFICA\u00C7\u00C3O E ASSINATURA DO RECEBEDOR";
    //
    private static final String LABEL_IDENTIFICACAO_EMITENTE = "IDENTIFICA\u00C7\u00C3O DO EMITENTE";
    private static final String LABEL_DANFE_SIGLA = "DANFE";
    private static final String LABEL_DANFE_DESCRICAO = "Documento Auxiliar da Nota Fiscal Eletr\u00F4nica";
    private static final String LABEL_TP_NF_ENTRADA = "0 - ENTRADA";
    private static final String LABEL_TP_NF_SAIDA = "1 - SA\u00CDDA";
    private static final String LABEL_CHAVE_ACESSO = "CHAVE DE ACESSO";
    private static final String LABEL_NATUREZA_OPERACAO = "NATUREZA DA OPERA\u00C7\u00C3O";
    private static final String LABEL_PROTOCOLO_AUTORIZACAO_USO = "PROTOCOLO DE AUTORIZA\u00C7\u00C3O DE USO";
    private static final String LABEL_PROTOCOLO_AUTORIZACAO_USO_EPEC = "PROTOCOLO DE AUTORIZA\u00C7\u00C3O DO EPEC";
    private static final String LABEL_INSCRICAO_ESTADUAL = "INSCRI\u00C7\u00C3O ESTADUAL";
    private static final String LABEL_INSCRICAO_ESTADUAL_SUBST_TRIBUT = "INSCRI\u00C7\u00C3O ESTADUAL DO SUBST. TRIBUT.";
    private static final String LABEL_CNPJ = "CNPJ";
    private static final String LABEL_CNPJ_CPF = "CNPJ/CPF";
    //
    private static final String LABEL_DESTINATARIO_REMETENTE = "DESTINAT\u00C1RIO / REMETENTE";
    private static final String LABEL_NOME_RAZAO_SOCIAL = "NOME / RAZ\u00C3O SOCIAL";
    private static final String LABEL_DT_EMISSAO = "DATA DA EMISS\u00C3O";
    private static final String LABEL_ENDERECO = "ENDERE\u00C7O";
    private static final String LABEL_BAIRRO = "BAIRRO";
    private static final String LABEL_CIDADE = "MUNIC\u00CDPIO";
    private static final String LABEL_UF = "UF";
    private static final String LABEL_CEP = "CEP";
    private static final String LABEL_FONE_FAX = "FONE / FAX";
    private static final String LABEL_DT_SAIDA_ENTRADA = "DATA DA SA\u00CDDA/ENTRADA";
    private static final String LABEL_HR_SAIDA_ENTRADA = "HORA DA SA\u00CDDA/ENTRADA";
    //
    private static final String LABEL_CALCULO_IMPOSTO = "C\u00C1LCULO DO IMPOSTO";
    private static final String LABEL_BASE_CALC_ICMS = "B.C\u00C1LC. ICMS";
    private static final String LABEL_BASE_CALC_ICMS_ST = "B.C\u00C1LC. ICMS S.T.";
    private static final String LABEL_VL_ICMS_SUBST = "V. ICMS SUBST.";
    private static final String LABEL_VL_II = "V. IMP. IMPORTA\u00C7\u00C3O";
    private static final String LABEL_VL_FCP = "VALOR DO FCP";
    private static final String LABEL_VL_PIS = "VALOR DO PIS";
    private static final String LABEL_VL_TOTAL_PRODUTOS = "V. TOTAL PRODUTOS";
    private static final String LABEL_VL_FRETE = "VALOR DO FRETE";
    private static final String LABEL_VL_SEGURO = "VALOR DO SEGURO";
    private static final String LABEL_DESCONTO = "DESCONTO";
    private static final String LABEL_OUTRAS_DESPESAS = "OUTRAS DESPESAS";
    private static final String LABEL_VL_TOTAL_IPI = "VALOR TOTAL IPI";
    private static final String LABEL_VL_ICMS_UF_REMET = "V. ICMS UF REMET.";
    private static final String LABEL_VL_ICMS_UF_DEST = "V. ICMS UF DEST.";
//    private static final String LABEL_VL_TOTAL_TRIBUTOS = "V. TOT. TRIB.";
    private static final String LABEL_VL_COFINS = "VALOR DA COFINS";
    private static final String LABEL_VL_TOTAL_NF = "V. TOTAL DA NOTA";
    //
    private static final String LABEL_TRANSPORTADOR_VOLUMES = "TRANSPORTADOR / VOLUMES TRANSPORTADOS";
    private static final String LABEL_FRETE_POR_CONTA = "FRETE POR CONTA";
    private static final String LABEL_CODIGO_ANTT = "C\u00D3DIGO ANTT";
    private static final String LABEL_PLACA_VEICULO = "PLACA DO VE\u00CDCULO";
    private static final String LABEL_QUANTIDADE = "QUANTIDADE";
    private static final String LABEL_ESPECIE = "ESP\u00C9CIE";
    private static final String LABEL_MARCA = "MARCA";
    private static final String LABEL_NUMERACAO = "NUMERA\u00C7\u00C3O";
    private static final String LABEL_PESO_BRUTO = "PESO BRUTO";
    private static final String LABEL_PESO_LIQUIDO = "PESO L\u00CDQUIDO";
    //
    private static final String LABEL_DADOS_PRODUTOS = "DADOS DOS PRODUTOS / SERVI\u00C7OS";
    private static final String LABEL_CODIGO_PRODUTO = "C\u00D3DIGO \n PRODUTO";
    private static final String LABEL_DESCRICAO_PRODUTO = "DESCRI\u00C7\u00C3O DOS PRODUTOS / SERVI\u00C7OS";
    private static final String LABEL_NCM = "NCM/SH";
    private static final String LABEL_O_CST = "CST";
    private static final String LABEL_CFOP = "CFOP";
    private static final String LABEL_UNIDADE = "UN";
    private static final String LABEL_QUANT = "QUANT";
    private static final String LABEL_VL_UNITARIO = "VALOR UNIT";
    private static final String LABEL_VL_TOTAL = "VALOR TOTAL";
    private static final String LABEL_VL_ICMS = "VALOR ICMS";
    private static final String LABEL_VL_IPI = "VALOR IPI";
    private static final String LABEL_ALIQUOTA_ICMS = "AL\u00CDQ ICMS";
    private static final String LABEL_ALIQUOTA_IPI = "AL\u00CDQ IPI";
    //
    private static final String LABEL_DADOS_ADICIONAIS = "DADOS ADICIONAIS";
    private static final String LABEL_INFO_COMPLEMENTAR = "INFORMA\u00C7\u00D5ES COMPLEMENTARES";
    private static final String LABEL_RESERVADO_FISCO = "RESERVADO AO FISCO";
    //
    private static final String OUTPUT_DATE_FORMAT = "dd/MM/yyyy";
    private static final String OUTPUT_TIME_FORMAT = "HH:mm:ss";
    private static final String OUTPUT_DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";
    private static final Font TITLE_FONT_CABEC = FontFactory.getFont(FontFactory.TIMES_ROMAN, 6);
    private static final Font DEFAULT_FONT_CABEC = FontFactory.getFont(FontFactory.TIMES_ROMAN, 7, Font.BOLD);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.TIMES_ROMAN, 5);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.TIMES_ROMAN, 6, Font.BOLD);
    private static final Font DEFAULT_FONT = FontFactory.getFont(FontFactory.TIMES_ROMAN, 6, Font.BOLD);
    private static final Font DANFE_FONT = FontFactory.getFont(FontFactory.TIMES_ROMAN, 14, Font.BOLD);
    private static final Font CHAVE_ACESSO_FONT = FontFactory.getFont(FontFactory.TIMES_ROMAN, 8, Font.BOLD);
    private static final Font DET_FONT = FontFactory.getFont(FontFactory.TIMES_ROMAN, 6);
//    private static final BaseColor CUSTOM_LIGHT_GRAY = new BaseColor(0xee, 0xee, 0xee);
//    private static final String STR_ZERO = "0.00";
    private static final float MARGIN_LEFT = 13f, MARGIN_RIGHT = 14f, MARGIN_TOP = 20f, MARGIN_BOTTOM = 30f;
    private static final float FOOTER_TITLE_HEIGHT = 10f;
    private static final float FOOTER_HEIGHT = 65f;

    @Autowired
    private DocumentoFiscalRepository documentoFiscalRepository;

    @Autowired
    private DocumentoClobRepository documentoClobRepository;

    @Autowired
    private EmissorRaizLogoRepository emissorRaizLogoRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    @Qualifier("nfeRetAutorizacaoContext")
    private JAXBContext context;

    @Override
    public Document fromChaveAcessoToPDF(String chaveAcesso, OutputStream outputStream) throws JAXBException {
        chaveAcesso = FazemuUtils.normalizarChaveAcesso(chaveAcesso);

        DocumentoFiscal documentoFiscal = documentoFiscalRepository.findByChaveAcesso(chaveAcesso);

        String xmlProcessado = null;
        if (documentoFiscal != null) {
            xmlProcessado = documentoClobRepository.getXmlRetornoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);
        }

        if (xmlProcessado == null) {
            xmlProcessado = documentoClobRepository.getMaxXmlEventoByIdDocFiscalAndTipoServico(documentoFiscal.getId(), TipoServicoEnum.AUTORIZACAO);
        }

        // Busca na AWS S3
        if (xmlProcessado == null) {
            xmlProcessado = storageService.recoverFromStorage(chaveAcesso, TipoServicoEnum.AUTORIZACAO.getTipoRetorno());
        }

        return xmlProcessado == null ? null : this.fromXmlNfeProcToPDF(xmlProcessado, outputStream);
    }

    @Override
    public Document fromXmlNfeProcToPDF(String xmlNfeProc, OutputStream outputStream) throws JAXBException {
        TNfeProc nfeProc = (TNfeProc) context.createUnmarshaller().unmarshal(new StringSource(xmlNfeProc));
        return this.fromNfeProcToPDF(nfeProc, outputStream, false);
    }

    @Override
    public Document fromNfeProcToPDF(TNfeProc nfeProc, OutputStream outputStream, Boolean isNFeExterna) {
        try {
            TNFe nfe = nfeProc.getNFe();

            Document document = new Document(PageSize.A4, MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP, MARGIN_BOTTOM + FOOTER_TITLE_HEIGHT + FOOTER_HEIGHT);
            PdfWriter pdfWriter = PdfWriter.getInstance(document, outputStream);
            pdfWriter.setPageEvent(new HeaderFooterDanfePageEvent(this, nfeProc, isNFeExterna));
            document.open(); //precisa abrir depois da criacao do PdfWriter

            document.addTitle(nfe.getInfNFe().getId());
//            cabecalho(pdfWriter, document, nfeProc); //ver HeaderFooterDanfePageEvent
            secaoDestinatarioRemetente(document, nfe);
            secaoCalculoDoImposto(document, nfe);
            secaoTransportadorVolumesTransportados(document, nfe);
            secaoDadosDosProdutosServicos(document, nfe);

            document.close();
            return document;

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new FazemuServiceException(e.getMessage());
        }
    }

    // secoes
    public void canhoto(PdfWriter pdfWriter, Document document, TNFe nfe) throws DocumentException {
        PdfPTable table;
        PdfPCell cell;
        Paragraph p;

        TEndereco enderDest = nfe.getInfNFe().getDest().getEnderDest();
        String enderecoNumeroComplemento = snvl(enderDest.getXLgr()) + StringUtils.SPACE + snvl(enderDest.getNro()) + StringUtils.SPACE + snvl(enderDest.getXCpl());

        table = buildTable(new int[]{6, 22, 7});
        table.setSpacingAfter(8f);

        addCell(table, "RECEBEMOS DE " + nfe.getInfNFe().getEmit().getXNome()
                + " OS PRODUTOS E/OU SERVI\u00C7OS CONSTANTES DA NOTA FISCAL ELETR\u00D4NICA INDICADA ABAIXO. EMISS\u00C3O " + cnvl(formatDateTime(nfe.getInfNFe().getIde().getDhEmi(), OUTPUT_DATE_FORMAT))
                + " VALOR TOTAL " + nfe.getInfNFe().getTotal().getICMSTot().getVNF()
                + " DESTINAT\u00C1RIO " + nfe.getInfNFe().getDest().getXNome()
                + StringUtils.SPACE + enderecoNumeroComplemento
                + StringUtils.SPACE + snvl(enderDest.getXBairro()) + StringUtils.SPACE + snvl(enderDest.getXMun()) + " - " + snvl(enderDest.getUF().value()),
                TITLE_FONT, StringUtils.EMPTY, DEFAULT_FONT, Paragraph.ALIGN_LEFT, 2);
        cell = new PdfPCell();
        cell.setRowspan(2);
        p = centeredParagraph(LABEL_NFE, DANFE_FONT);
        p.setSpacingAfter(4f);
        cell.addElement(p);
        cell.addElement(centeredParagraph(LABEL_NNF + FazemuUtils.maskNFeNumber(nfe.getInfNFe().getIde().getNNF()), DEFAULT_FONT_CABEC));
        cell.addElement(centeredParagraph(LABEL_SERIE + nfe.getInfNFe().getIde().getSerie(), DEFAULT_FONT_CABEC));
        table.addCell(cell);
        addCell(table, LABEL_DT_RECEBIMENTO, TITLE_FONT, StringUtils.LF, DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_IDENTIF_RECEBEDOR, TITLE_FONT, StringUtils.LF, DEFAULT_FONT, Paragraph.ALIGN_LEFT);

        DottedLineSeparator lineSeparator = new DottedLineSeparator();
        lineSeparator.setOffset(-5f);
        lineSeparator.setLineWidth(1f);
        cell = new PdfPCell();
        cell.setColspan(3);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.addElement(lineSeparator);
        table.addCell(cell);

        document.add(table);
    }

    public void cabecalho(PdfWriter pdfWriter, Document document, TNfeProc nfeProc, Boolean isNFeExterna) throws Exception {
        PdfPTable table;

        TNFe nfe = nfeProc.getNFe();
        Emit emit = nfe.getInfNFe().getEmit();

        table = buildTable(new int[]{7, 3, 8});
        table.addCell(celulaIdentificacaoDoEmitente(nfe, isNFeExterna));
        table.addCell(celulaDANFE(pdfWriter, nfe));
        table.addCell(celulaCodigoDeBarras(pdfWriter, nfe));
        this.celulaChaveDeAcesso(nfe, table);
        table.addCell(celulaConsultaDeAutenticidade());

        String nProt = "", dhRecbto = "", protocoloAutorizacaoUso = "";
        if (nfeProc.getProtNFe() != null) {
            nProt = nfeProc.getProtNFe().getInfProt().getNProt();
            dhRecbto = formatDateTime(nfeProc.getProtNFe().getInfProt().getDhRecbto(), OUTPUT_DATETIME_FORMAT);
            protocoloAutorizacaoUso = StringUtils.isBlank(nProt) ? dhRecbto : nProt + " - " + dhRecbto;
        }

        //second row
        addCell(table, LABEL_NATUREZA_OPERACAO, TITLE_FONT_CABEC, cnvl(nfe.getInfNFe().getIde().getNatOp()), DEFAULT_FONT_CABEC, Paragraph.ALIGN_CENTER, 2);
        if (!TipoEmissaoEnum.EPEC.getCodigo().toString().equals(nfeProc.getNFe().getInfNFe().getIde().getTpEmis())) {
            addCell(table, LABEL_PROTOCOLO_AUTORIZACAO_USO, TITLE_FONT_CABEC, protocoloAutorizacaoUso, DEFAULT_FONT_CABEC, Paragraph.ALIGN_CENTER, 1);
        } else {
            addCell(table, LABEL_PROTOCOLO_AUTORIZACAO_USO_EPEC, TITLE_FONT_CABEC, protocoloAutorizacaoUso, DEFAULT_FONT_CABEC, Paragraph.ALIGN_CENTER, 1);
        }

        document.add(table);

        table = buildTable(new int[]{1, 1, 1});
        addCell(table, LABEL_INSCRICAO_ESTADUAL, TITLE_FONT_CABEC, cnvl(emit.getIE()), DEFAULT_FONT_CABEC);
        addCell(table, LABEL_INSCRICAO_ESTADUAL_SUBST_TRIBUT, TITLE_FONT_CABEC, cnvl(emit.getIEST()), DEFAULT_FONT_CABEC);
        addCell(table, LABEL_CNPJ, TITLE_FONT_CABEC, cnvl(FazemuUtils.maskCNPJorCPF(emit.getCNPJ(), emit.getCPF())), DEFAULT_FONT_CABEC);

        document.add(table);
    }

    public void rodape(PdfWriter pdfWriter, Document document, TNFe nfe) throws DocumentException, ParseException {
        if (document.getPageNumber() == 1) {
            PdfPCell cell;
            PdfPTable table = buildTable(new int[]{2, 1});
            table.setTotalWidth(document.right() - document.left());

            InfAdic infAdic = nfe.getInfNFe().getInfAdic();
//            ICMSTot icmsTot = nfe.getInfNFe().getTotal().getICMSTot();

            StringBuilder sbInfCpl = new StringBuilder();
            //for (ObsCont obsCont: infAdic.getObsCont()) {
            //    sbInfCpl.append(StringUtils.LF).append(obsCont.getXCampo()).append(": ").append(obsCont.getXTexto());
            //}
            //sbInfCpl.append(StringUtils.LF).append(FazemuUtils.APLICACAO);

            Integer tipoAmbiente = parametrosInfraRepository.getAsInteger(null, ParametrosInfraRepository.PAIN_TIPO_AMBIENTE);
            if (tipoAmbiente.equals(2)) {
                sbInfCpl.append(StringUtils.LF).append("===SEM VALOR FISCAL===");
            }

            sbInfCpl.append(StringUtils.LF).append(snvl(infAdic.getInfCpl()));

            cell = addCell(table, LABEL_DADOS_ADICIONAIS, SECTION_FONT, StringUtils.EMPTY, DEFAULT_FONT, Paragraph.ALIGN_LEFT, table.getNumberOfColumns());
            cell.setFixedHeight(FOOTER_TITLE_HEIGHT);
            cell.setBorder(PdfPCell.NO_BORDER);

            cell = new PdfPCell();
            cell.setFixedHeight(FOOTER_HEIGHT);
            cell.setPaddingTop(0f);
            cell.addElement(new Paragraph(LABEL_INFO_COMPLEMENTAR, SECTION_FONT));
            cell.addElement(new Paragraph(snvl(sbInfCpl.toString()), TITLE_FONT));
            table.addCell(cell);

            cell = new PdfPCell();
            cell.setFixedHeight(FOOTER_HEIGHT);
            cell.setPaddingTop(0f);
            cell.addElement(new Paragraph(LABEL_RESERVADO_FISCO, SECTION_FONT));
            table.addCell(cell);

            LOGGER.debug("margins left {} right {} top {} bottom {} doc left {} right {} top {} bottom {} table height {}",
                    document.leftMargin(), document.rightMargin(), document.topMargin(), document.bottomMargin(),
                    document.left(), document.right(), document.top(), document.bottom(),
                    table.getTotalHeight()
            );

            PdfPTable table1 = buildTable(new int[]{1});
            table1.setTotalWidth(document.right() - document.left());
            if (!TipoEmissaoEnum.NORMAL.getCodigo().toString().equals(nfe.getInfNFe().getIde().getTpEmis())) {

                PdfPCell cellContingencia = spaceTableContingencia(table, document, Integer.valueOf(nfe.getInfNFe().getIde().getTpEmis()), nfe.getInfNFe().getIde().getXJust(), DateUtils.iso8601ToCalendar(nfe.getInfNFe().getIde().getDhCont()).getTime());
//            	cellContingencia.setRowspan(2);
                cellContingencia.setBorder(PdfPCell.NO_BORDER);
                table.addCell(cellContingencia);

                cellContingencia = new PdfPCell();
                cellContingencia.setBorder(PdfPCell.NO_BORDER);
                table.addCell(cellContingencia);
            }

            table.writeSelectedRows(0, -1, document.left(), table.getTotalHeight() + MARGIN_BOTTOM, pdfWriter.getDirectContent());
        }
    }

    protected PdfPCell spaceTableContingencia(PdfPTable table, Document document, Integer tipoEmissao, String justificativa, Date dataHora) throws DocumentException, ParseException {
//        PdfPTable spaceTable = buildTable(new int[]{1});
        PdfPCell cell = new PdfPCell();

        String avisoContingencia = null;
        if (TipoEmissaoEnum.EPEC.getCodigo().equals(tipoEmissao)) {
            avisoContingencia = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_DANFE_AVISO_EPEC);
        } else {
            avisoContingencia = parametrosInfraRepository.getAsString(TIPO_DOCUMENTO_FISCAL_NFE, ParametrosInfraRepository.PAIN_DANFE_AVISO_SVC);
        }

        StringBuilder sb = new StringBuilder(avisoContingencia)
                .append(StringUtils.LF)
                .append("Justificativa Contingência: " + justificativa + " | Data Hora Contingência: " + DateUtils.convertDateToString(dataHora));

        cell.addElement(defaultParagraph(sb.toString(), DET_FONT));
        return cell;
    }

    protected void secaoDestinatarioRemetente(Document document, TNFe nfe) throws DocumentException {
        PdfPTable table;

        Dest dest = nfe.getInfNFe().getDest();
        TEndereco enderDest = dest.getEnderDest();
        String enderecoNumeroComplemento = cnvl(StringUtils.joinWith(StringUtils.SPACE, snvl(enderDest.getXLgr()), snvl(enderDest.getNro()), snvl(enderDest.getXCpl())));

        //linha titulo
        table = buildTable(new int[]{1});
        PdfPCell c = addCell(table, LABEL_DESTINATARIO_REMETENTE, SECTION_FONT, Paragraph.ALIGN_LEFT);
        c.setBorder(PdfPCell.NO_BORDER);
        document.add(table);

        table = buildTable(new int[]{11, 5, 3});
        addCell(table, LABEL_NOME_RAZAO_SOCIAL, TITLE_FONT, cnvl(dest.getXNome()), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_CNPJ_CPF, TITLE_FONT, cnvl(FazemuUtils.maskCNPJorCPF(dest.getCNPJ(), dest.getCPF())), DEFAULT_FONT);
        addCell(table, LABEL_DT_EMISSAO, TITLE_FONT, cnvl(formatDateTime(nfe.getInfNFe().getIde().getDhEmi(), OUTPUT_DATETIME_FORMAT)), DEFAULT_FONT);
        document.add(table);

        table = buildTable(new int[]{9, 4, 3, 3});
        addCell(table, LABEL_ENDERECO, TITLE_FONT, enderecoNumeroComplemento, DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_BAIRRO, TITLE_FONT, cnvl(enderDest.getXBairro()), DEFAULT_FONT);
        String cep = enderDest.getCEP() != null && !"".equals(enderDest.getCEP()) ? FazemuUtils.formatString(StringUtils.leftPad(enderDest.getCEP(), 8, "0"), FazemuUtils.MASCARA_CEP) : "";
        addCell(table, LABEL_CEP, TITLE_FONT, cnvl(cep), DEFAULT_FONT, Paragraph.ALIGN_CENTER);
        addCell(table, LABEL_DT_SAIDA_ENTRADA, TITLE_FONT, cnvl(formatDateTime(nfe.getInfNFe().getIde().getDhSaiEnt(), OUTPUT_DATE_FORMAT)), DEFAULT_FONT);
        document.add(table);

        table = buildTable(new int[]{9, 1, 3, 3, 3});
        addCell(table, LABEL_CIDADE, TITLE_FONT, cnvl(enderDest.getXMun()), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_UF, TITLE_FONT, cnvl(enderDest.getUF().value()), DEFAULT_FONT);
        addCell(table, LABEL_FONE_FAX, TITLE_FONT, cnvl(enderDest.getFone()), DEFAULT_FONT);
        addCell(table, LABEL_INSCRICAO_ESTADUAL, TITLE_FONT, cnvl(dest.getIE()), DEFAULT_FONT);
        addCell(table, LABEL_HR_SAIDA_ENTRADA, TITLE_FONT, cnvl(formatDateTime(nfe.getInfNFe().getIde().getDhSaiEnt(), OUTPUT_TIME_FORMAT)), DEFAULT_FONT);
        document.add(table);
    }

    protected void secaoCalculoDoImposto(Document document, TNFe nfe) throws DocumentException {
        PdfPTable table;
        PdfPTable table2;

        ICMSTot icmsTot = nfe.getInfNFe().getTotal().getICMSTot();

        table = buildTable(new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1});
        //linha titulo
        PdfPCell c = addCell(table, LABEL_CALCULO_IMPOSTO, SECTION_FONT, Paragraph.ALIGN_LEFT, table.getNumberOfColumns());
        c.setBorder(PdfPCell.NO_BORDER);
        //primeira linha de 9
        addCell(table, LABEL_BASE_CALC_ICMS, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVBC())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table, LABEL_VL_ICMS, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVICMS())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table, LABEL_BASE_CALC_ICMS_ST, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVBCST())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table, LABEL_VL_ICMS_SUBST, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVST())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table, LABEL_VL_II, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVII())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table, LABEL_VL_ICMS_UF_REMET, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVICMSUFRemet())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table, LABEL_VL_FCP, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVFCP())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table, LABEL_VL_PIS, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVPIS())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table, LABEL_VL_TOTAL_PRODUTOS, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVProd())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);

        //segunda linha de 8
        table2 = buildTable(new int[]{1, 1, 1, 1, 1, 1, 1, 1});

        addCell(table2, LABEL_VL_FRETE, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVFrete())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table2, LABEL_VL_SEGURO, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVSeg())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table2, LABEL_DESCONTO, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVDesc())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table2, LABEL_OUTRAS_DESPESAS, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVOutro())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table2, LABEL_VL_TOTAL_IPI, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVIPI())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table2, LABEL_VL_ICMS_UF_DEST, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVICMSUFDest())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        //addCell(table2, LABEL_VL_TOTAL_TRIBUTOS, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVTotTrib())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table2, LABEL_VL_COFINS, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVCOFINS())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        addCell(table2, LABEL_VL_TOTAL_NF, TITLE_FONT, FazemuUtils.formatMoney(vnvl(icmsTot.getVNF())), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);

        document.add(table);
        document.add(table2);
    }

    protected void secaoTransportadorVolumesTransportados(Document document, TNFe nfe) throws DocumentException {
        PdfPTable table;

        Transp transp = nfe.getInfNFe().getTransp();
        Transporta transportadora = transp == null ? null : transp.getTransporta();
        TVeiculo veiculo = transp == null ? null : transp.getVeicTransp();

        //linha titulo
        table = buildTable(new int[]{1});
        PdfPCell c = addCell(table, LABEL_TRANSPORTADOR_VOLUMES, SECTION_FONT, Paragraph.ALIGN_LEFT);
        c.setBorder(PdfPCell.NO_BORDER);
        document.add(table);
        //linha 1
        String modFrete = transp == null ? null : transp.getModFrete();
        if (modFrete != null) {
            if (modFrete == "0") {
                modFrete = "Emitente " + modFrete;
            } else if (modFrete == "1") {
                modFrete = "Destinatário " + modFrete;
            } else if (modFrete == "2") {
                modFrete = "Terceiros " + modFrete;
            } else if (modFrete == "9") {
                modFrete = "Sem frete " + modFrete;
            }
        }

        table = buildTable(new int[]{6, 3, 3, 3, 1, 4});
        addCell(table, LABEL_NOME_RAZAO_SOCIAL, TITLE_FONT, cnvl(transportadora == null ? null : transportadora.getXNome()), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_FRETE_POR_CONTA, TITLE_FONT, cnvl(modFrete), DEFAULT_FONT);
        addCell(table, LABEL_CODIGO_ANTT, TITLE_FONT, cnvl(veiculo == null ? null : veiculo.getRNTC()), DEFAULT_FONT);
        addCell(table, LABEL_PLACA_VEICULO, TITLE_FONT, cnvl(veiculo == null ? null : veiculo.getPlaca()), DEFAULT_FONT);
        addCell(table, LABEL_UF, TITLE_FONT, cnvl(veiculo == null ? null : (veiculo.getUF() == null ? null : veiculo.getUF().value())), DEFAULT_FONT);
        addCell(table, LABEL_CNPJ_CPF, TITLE_FONT, cnvl(transportadora == null ? null : transportadora.getCNPJ()), DEFAULT_FONT);
        document.add(table);
        //linha 2
        table = buildTable(new int[]{9, 6, 1, 4});
        addCell(table, LABEL_ENDERECO, TITLE_FONT, cnvl(transportadora == null ? null : transportadora.getXEnder()), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_CIDADE, TITLE_FONT, cnvl(transportadora == null ? null : transportadora.getXMun()), DEFAULT_FONT, Paragraph.ALIGN_LEFT);
        addCell(table, LABEL_UF, TITLE_FONT, cnvl(transportadora == null ? null : (transportadora.getUF() == null ? null : transportadora.getUF().value())), DEFAULT_FONT);
        addCell(table, LABEL_INSCRICAO_ESTADUAL, TITLE_FONT, cnvl(transportadora == null ? null : transportadora.getIE()), DEFAULT_FONT);
        document.add(table);
        //linha 3
        table = buildTable(new int[]{4, 7, 7, 7, 7, 7});
        if (transp != null && CollectionUtils.isNotEmpty(transp.getVol())) {
            Vol vol = transp.getVol().get(0);
            addCell(table, LABEL_QUANTIDADE, TITLE_FONT, cnvl(vol == null ? null : vol.getQVol()), DEFAULT_FONT);
            addCell(table, LABEL_ESPECIE, TITLE_FONT, cnvl(vol == null ? null : vol.getEsp()), DEFAULT_FONT);
            addCell(table, LABEL_MARCA, TITLE_FONT, cnvl(vol == null ? null : vol.getMarca()), DEFAULT_FONT);
            addCell(table, LABEL_NUMERACAO, TITLE_FONT, cnvl(vol == null ? null : vol.getNVol()), DEFAULT_FONT);
            addCell(table, LABEL_PESO_BRUTO, TITLE_FONT, cnvl(vol == null ? null : vol.getPesoB()), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
            addCell(table, LABEL_PESO_LIQUIDO, TITLE_FONT, cnvl(vol == null ? null : vol.getPesoL()), DEFAULT_FONT, Paragraph.ALIGN_RIGHT);
        } else {
            addCell(table, LABEL_QUANTIDADE, TITLE_FONT, StringUtils.LF, DEFAULT_FONT);
            addCell(table, LABEL_ESPECIE, TITLE_FONT, StringUtils.LF, DEFAULT_FONT);
            addCell(table, LABEL_MARCA, TITLE_FONT, StringUtils.LF, DEFAULT_FONT);
            addCell(table, LABEL_NUMERACAO, TITLE_FONT, StringUtils.LF, DEFAULT_FONT);
            addCell(table, LABEL_PESO_BRUTO, TITLE_FONT, StringUtils.LF, DEFAULT_FONT);
            addCell(table, LABEL_PESO_LIQUIDO, TITLE_FONT, StringUtils.LF, DEFAULT_FONT);
        }
        document.add(table);
    }

    protected void secaoDadosDosProdutosServicos(Document document, TNFe nfe) throws DocumentException {
        PdfPTable table = buildTable(new int[]{5, 20, 5, 4, 4, 3, 4, 5, 6, 4, 4, 4, 4, 4});
        table.setHeaderRows(2); //se houver quebra de pagina, as duas primeiras linhas aparecem na pagina seguinte
        PdfPCell c = addCell(table, LABEL_DADOS_PRODUTOS, SECTION_FONT, Paragraph.ALIGN_LEFT, table.getNumberOfColumns());
        c.setBorder(PdfPCell.NO_BORDER);
        for (String text : new String[]{LABEL_CODIGO_PRODUTO, LABEL_DESCRICAO_PRODUTO, LABEL_NCM, LABEL_O_CST, LABEL_CFOP,
            LABEL_UNIDADE, LABEL_QUANT, LABEL_VL_UNITARIO, LABEL_VL_TOTAL, LABEL_BASE_CALC_ICMS, LABEL_VL_ICMS, LABEL_VL_IPI, LABEL_ALIQUOTA_ICMS, LABEL_ALIQUOTA_IPI}) {
            addCell(table, text, TITLE_FONT);
        }

//        for (int k = 0; k < 50; k++) {
        for (Det det : nfe.getInfNFe().getDet()) {
            Prod prod = det.getProd();

            DanfeDetalheForm danfeDetalheForm = new DanfeDetalheForm();
            danfeDetalheForm.setCodigoProduto(prod.getCProd());
            danfeDetalheForm.setDescricao(prod.getXProd());
            danfeDetalheForm.setNCM(prod.getNCM());
            danfeDetalheForm.setCFOP(prod.getCFOP());
            danfeDetalheForm.setUnidade(prod.getUCom());
            danfeDetalheForm.setQuantidade(prod.getQCom());
            danfeDetalheForm.setValorUnitario(prod.getVUnCom());
            danfeDetalheForm.setValorTotal(prod.getVProd());

            if (det.getInfAdProd() != null) {
                danfeDetalheForm.setInfoAdicionalDescricao(det.getInfAdProd());
                danfeDetalheForm.setDescricao(danfeDetalheForm.getDescricao() + danfeDetalheForm.getInfoAdicionalDescricao());
            }

            for (JAXBElement<?> obj : det.getImposto().getContent()) {
                if (obj.getValue() instanceof ICMS) {
                    ICMS icms = (ICMS) obj.getValue();

                    if (icms.getICMS00() != null) {
                        danfeDetalheForm.setCST(icms.getICMS00().getOrig() + icms.getICMS00().getCST());
                        danfeDetalheForm.setBaseICMS(icms.getICMS00().getVBC());
                        danfeDetalheForm.setValorICMS(icms.getICMS00().getVICMS());
                        danfeDetalheForm.setAliquotaICMS(icms.getICMS00().getPICMS());
                    } else if (icms.getICMS10() != null) {
                        danfeDetalheForm.setCST(icms.getICMS10().getOrig() + icms.getICMS10().getCST());
                        danfeDetalheForm.setBaseICMS(icms.getICMS10().getVBC());
                        danfeDetalheForm.setValorICMS(icms.getICMS10().getVICMS());
                        danfeDetalheForm.setAliquotaICMS(icms.getICMS10().getPICMS());
                    } else if (icms.getICMS20() != null) {
                        danfeDetalheForm.setCST(icms.getICMS20().getOrig() + icms.getICMS20().getCST());
                        danfeDetalheForm.setBaseICMS(icms.getICMS20().getVBC());
                        danfeDetalheForm.setValorICMS(icms.getICMS20().getVICMS());
                        danfeDetalheForm.setAliquotaICMS(icms.getICMS20().getPICMS());
                    } else if (icms.getICMS30() != null) {
                        danfeDetalheForm.setCST(icms.getICMS30().getOrig() + icms.getICMS30().getCST());
                    } else if (icms.getICMS40() != null) {
                        danfeDetalheForm.setCST(icms.getICMS40().getOrig() + icms.getICMS40().getCST());
                    } else if (icms.getICMS51() != null) {
                        danfeDetalheForm.setCST(icms.getICMS51().getOrig() + icms.getICMS51().getCST());
                        danfeDetalheForm.setBaseICMS(icms.getICMS51().getVBC());
                        danfeDetalheForm.setValorICMS(icms.getICMS51().getVICMS());
                        danfeDetalheForm.setAliquotaICMS(icms.getICMS51().getPICMS());
                    } else if (icms.getICMS60() != null) {
                        danfeDetalheForm.setCST(icms.getICMS60().getOrig() + icms.getICMS60().getCST());
                    } else if (icms.getICMS70() != null) {
                        danfeDetalheForm.setCST(icms.getICMS70().getOrig() + icms.getICMS70().getCST());
                        danfeDetalheForm.setBaseICMS(icms.getICMS70().getVBC());
                        danfeDetalheForm.setValorICMS(icms.getICMS70().getVICMS());
                        danfeDetalheForm.setAliquotaICMS(icms.getICMS70().getPICMS());
                    } else if (icms.getICMS90() != null) {
                        danfeDetalheForm.setCST(icms.getICMS90().getOrig() + icms.getICMS90().getCST());
                        danfeDetalheForm.setBaseICMS(icms.getICMS90().getVBC());
                        danfeDetalheForm.setValorICMS(icms.getICMS90().getVICMS());
                        danfeDetalheForm.setAliquotaICMS(icms.getICMS90().getPICMS());
                    } else {
                        danfeDetalheForm.setCST(StringUtils.EMPTY);
                        danfeDetalheForm.setBaseICMS(StringUtils.EMPTY);
                        danfeDetalheForm.setValorICMS(StringUtils.EMPTY);
                        danfeDetalheForm.setAliquotaICMS(StringUtils.EMPTY);
                    }
                    danfeDetalheForm.setValorIPI(StringUtils.EMPTY);
                    danfeDetalheForm.setAliquotaIPI(StringUtils.EMPTY);
                }
            }
            addCell(table, cnvl(danfeDetalheForm.getCodigoProduto()), DET_FONT);
            addCell(table, cnvl(danfeDetalheForm.getDescricao()), DET_FONT, Paragraph.ALIGN_LEFT);
            addCell(table, cnvl(danfeDetalheForm.getNCM()), DET_FONT);
            addCell(table, cnvl(danfeDetalheForm.getCST()), DET_FONT);
            addCell(table, cnvl(danfeDetalheForm.getCFOP()), DET_FONT);
            addCell(table, cnvl(danfeDetalheForm.getUnidade()), DET_FONT);
            addCell(table, cnvl(danfeDetalheForm.getQuantidade()), DET_FONT);
            addCell(table, cnvl(FazemuUtils.formatMoney(danfeDetalheForm.getValorUnitario())), DET_FONT, Paragraph.ALIGN_RIGHT);
            addCell(table, cnvl(FazemuUtils.formatMoney(danfeDetalheForm.getValorTotal())), DET_FONT, Paragraph.ALIGN_RIGHT);
            addCell(table, FazemuUtils.formatMoney(vnvl(danfeDetalheForm.getBaseICMS())), DET_FONT, Paragraph.ALIGN_RIGHT);
            addCell(table, FazemuUtils.formatMoney(vnvl(danfeDetalheForm.getValorICMS())), DET_FONT, Paragraph.ALIGN_RIGHT);
            addCell(table, vnvl(danfeDetalheForm.getValorIPI()), DET_FONT, Paragraph.ALIGN_RIGHT);
            addCell(table, FazemuUtils.formatMoney(vnvl(danfeDetalheForm.getAliquotaICMS())), DET_FONT, Paragraph.ALIGN_RIGHT);
            addCell(table, vnvl(danfeDetalheForm.getAliquotaIPI()), DET_FONT, Paragraph.ALIGN_RIGHT);

        }
//        } //for k -- emulando muitas linhas na nota pra ver quebra da pagina

        //zebrando linhas
//        boolean blnZebra = true;
//        for (PdfPRow row: table.getRows()) {
//            if (blnZebra) {
//                for (PdfPCell cell: row.getCells()) {
//                    cell.setBackgroundColor(CUSTOM_LIGHT_GRAY);
//                }
//            }
//            blnZebra = !blnZebra;
//        }
        document.add(table);
    }

    // celulas especiais
    protected PdfPCell celulaIdentificacaoDoEmitente(TNFe nfe, Boolean isNFeExterna) throws Exception {
        TEnderEmi end = nfe.getInfNFe().getEmit().getEnderEmit();
        Paragraph p;
        PdfPCell cell = new PdfPCell();
        cell.setRowspan(3);

        Image logo = isNFeExterna ? this.obterLogoEmitenteFromBase64(nfe) : this.obterLogoEmitente(nfe);

        if (logo == null) {
            p = centeredParagraph(LABEL_IDENTIFICACAO_EMITENTE, TITLE_FONT);
            p.setSpacingAfter(10f);
            cell.addElement(p);
        } else {
            logo.setScaleToFitHeight(false);
            logo.setScaleToFitLineWhenOverflow(false);
            logo.scaleToFit(100f, 75f);
            logo.setAlignment(Image.MIDDLE);
            cell.addElement(logo);
        }

        cell.addElement(centeredParagraph(nfe.getInfNFe().getEmit().getXNome(), DEFAULT_FONT));
        cell.addElement(centeredParagraph(StringUtils.joinWith(StringUtils.SPACE, snvl(end.getXLgr()), snvl(end.getNro()), snvl(end.getXCpl())), TITLE_FONT));
        cell.addElement(centeredParagraph(StringUtils.joinWith(" - ", snvl(end.getXBairro()), snvl(end.getCEP()), snvl(end.getXMun()), end.getUF().value()), TITLE_FONT));
        return cell;
    }

    protected PdfPCell celulaDANFE(PdfWriter pdfWriter, TNFe nfe) throws DocumentException {
        Paragraph p;
        PdfPCell cell = new PdfPCell(), internalCell;
        cell.setRowspan(3);
        cell.addElement(centeredParagraph(LABEL_DANFE_SIGLA, DANFE_FONT));
        p = centeredParagraph(LABEL_DANFE_DESCRICAO, TITLE_FONT_CABEC);
        p.setSpacingAfter(4f);
        cell.addElement(p);

        PdfPTable internalTable = buildTable(new int[]{7, 1, 3, 1});
        //
        internalCell = new PdfPCell();
        internalCell.setBorder(PdfPCell.NO_BORDER);
        internalCell.addElement(new Paragraph(LABEL_TP_NF_ENTRADA, TITLE_FONT_CABEC));
        internalCell.addElement(new Paragraph(LABEL_TP_NF_SAIDA, TITLE_FONT_CABEC));
        internalTable.addCell(internalCell);
        //
        internalCell = new PdfPCell();
        internalCell.setBorder(PdfPCell.NO_BORDER);
        internalTable.addCell(internalCell);
        //
        internalCell = new PdfPCell();
        internalCell.setVerticalAlignment(PdfPCell.ALIGN_TOP);
        internalCell.addElement(centeredParagraph(nfe.getInfNFe().getIde().getTpNF(), DEFAULT_FONT_CABEC));
        internalTable.addCell(internalCell);
        //
        internalCell = new PdfPCell();
        internalCell.setBorder(PdfPCell.NO_BORDER);
        internalTable.addCell(internalCell);
        //
        cell.addElement(internalTable);

        p = centeredParagraph(LABEL_NNF + FazemuUtils.maskNFeNumber(nfe.getInfNFe().getIde().getNNF()), DEFAULT_FONT_CABEC);
        p.setSpacingBefore(4f);
        cell.addElement(p);
        cell.addElement(centeredParagraph(LABEL_SERIE + nfe.getInfNFe().getIde().getSerie(), DEFAULT_FONT_CABEC));
        cell.addElement(centeredParagraph(LABEL_FOLHA + pdfWriter.getCurrentPageNumber(), TITLE_FONT_CABEC));
        return cell;
    }

    protected PdfPCell celulaCodigoDeBarras(PdfWriter pdfWriter, TNFe nfe) {
        String chaveAcesso = nfe.getInfNFe().getId();
        int index = chaveAcesso.startsWith(FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE) ? FazemuUtils.TIPO_DOCUMENTO_FISCAL_NFE.length() : 0;
        Barcode128 barcode = new Barcode128();
        barcode.setCode(chaveAcesso.substring(index));
        barcode.setFont(null);
        barcode.setBarHeight(40f);
        PdfPCell cell = new PdfPCell();
        cell.addElement(barcode.createImageWithBarcode(pdfWriter.getDirectContent(), BaseColor.BLACK, BaseColor.BLACK));
        cell.setPaddingTop(5f);
        cell.setPaddingBottom(5f);
        cell.setPaddingLeft(15f);
        cell.setPaddingRight(15f);
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
        return addCell(table, LABEL_CHAVE_ACESSO, TITLE_FONT, sb.substring(1), CHAVE_ACESSO_FONT, Paragraph.ALIGN_CENTER);
    }

    protected PdfPCell celulaConsultaDeAutenticidade() {
        PdfPCell cell = new PdfPCell();
        cell.addElement(centeredParagraph("Consulta de autenticidade no portal nacional da NF-e", SECTION_FONT));
        cell.addElement(centeredParagraph("www.nfe.fazenda.gov.br/portal ou no site da Sefaz Autorizadora", SECTION_FONT));
        return cell;
    }

    protected Image obterLogoEmitente(TNFe nfe) {
        try {
            String idLogo = null;

            List<ObsCont> obsContList = nfe.getInfNFe().getInfAdic().getObsCont();
            for (ObsCont obsCont : obsContList) {
                if ("Logo".equalsIgnoreCase(obsCont.getXCampo())) {
                    idLogo = obsCont.getXTexto();
                    break; //campo de informacoes adicionais com o id do logo encontrado; encerra o loop
                }
            }

            if (idLogo != null) {
                Long idEmissorRaiz = FazemuUtils.obterRaizCNPJ(Long.valueOf(nfe.getInfNFe().getEmit().getCNPJ()));
                EmissorRaizLogo emissorRaizLogo = emissorRaizLogoRepository.findByIdEmissorRaizAndIdLogo(idEmissorRaiz, idLogo);
                if (emissorRaizLogo != null) {
                    return Image.getInstance(emissorRaizLogo.getLogo());
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (BadElementException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    protected Image obterLogoEmitenteFromBase64(TNFe nfe) throws Exception {
        try {
            String idLogo = null;

            List<ObsCont> obsContList = nfe.getInfNFe().getInfAdic().getObsCont();
            for (ObsCont obsCont : obsContList) {
                if ("Logo".equalsIgnoreCase(obsCont.getXCampo())) {
                    idLogo = obsCont.getXTexto().replace("\n", "");		//remove quebra de linha
                    break; //campo de informacoes adicionais com o id do logo encontrado; encerra o loop
                }
            }

            if (idLogo != null) {
                byte[] decodedBytes = Base64.getDecoder().decode(idLogo);
                return Image.getInstance(decodedBytes);
            }

        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new Exception("Nao foi possivel obter o logo em base 64: " + e.getMessage());
        } catch (BadElementException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

}
