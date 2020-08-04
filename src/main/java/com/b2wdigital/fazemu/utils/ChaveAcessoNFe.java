package com.b2wdigital.fazemu.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.el.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chave Acesso NFe.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
public class ChaveAcessoNFe {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChaveAcessoNFe.class);

    public String cUF;
    public String dataAAMM;
    public String cnpjCpf;
    public String mod;
    public String serie;
    public String nNF;
    public String tpEmis;
    public String cNF;
    public String cDV;

    /**
     * ChaveAcessoNFe
     */
    public ChaveAcessoNFe() {
        // DO NOTHING
    }

    /**
     * ChaveAcessoNFe
     *
     * @param chaveAcesso
     * @throws ParseException
     */
    public ChaveAcessoNFe(String chaveAcesso) throws ParseException {
        unparseKey(chaveAcesso);
    }

    /**
     * ChaveAcessoNFe
     *
     * @param cUF
     * @param dataAAMM
     * @param cnpjCpf
     * @param mod
     * @param serie
     * @param nNF
     * @param tpEmis
     * @param cNF
     * @throws ParseException
     */
    public ChaveAcessoNFe(String cUF, String dataAAMM, String cnpjCpf, String mod, String serie,
            String nNF, String tpEmis, String cNF) throws ParseException {
        this.cUF = cUF;
        this.dataAAMM = dataAAMM;
        this.cnpjCpf = cnpjCpf;
        this.mod = mod;
        this.serie = serie;
        this.nNF = nNF;
        this.tpEmis = tpEmis;
        this.cNF = cNF;
        this.cDV = String.valueOf(calculateDV(parseKey(cUF, dataAAMM, cnpjCpf, mod, serie, nNF, tpEmis, cNF, false)));
    }

    /**
     * calculate DV
     *
     * @param cUF Código da UF do emitente do Documento Fiscal
     * @param dataAAMM Ano e Mês de emissão da NF-e
     * @param cnpjCpf CNPJ do emitente
     * @param mod Modelo do Documento Fiscal
     * @param serie Série do Documento Fiscal
     * @param nNF Número do Documento Fiscal
     * @param tpEmis Forma de emissão da NF-e
     * @param cNF Código Numérico que compõe a Chave de Acesso
     *
     * @return digitoVerificador
     * @throws ParseException
     */
    public static int calculateDV(String cUF, String dataAAMM, String cnpjCpf, String mod, String serie, String nNF,
            String tpEmis, String cNF) throws ParseException {

        String chave = parseKey(cUF, dataAAMM, cnpjCpf, mod, serie, nNF, tpEmis, cNF, false);

        return calculateDV(chave);
    }

    /**
     * calculate DV
     *
     * @param key43Positions
     * @return digitoVerificador
     * @throws ParseException
     */
    public static int calculateDV(String key43Positions) throws ParseException {
        if (key43Positions == null) {
            throw new ParseException("Chave de Acesso não pode ser nula");
        }
        if (key43Positions.length() != 43) {
            throw new ParseException("Chave de Acesso deve ter 43 ou 44 posições");
        }

        return modulo11(key43Positions.toString());
    }

    /**
     * Parse Key
     *
     * @param cUF Código da UF do emitente do Documento Fiscal
     * @param dataAAMM Ano e Mês de emissão da NF-e
     * @param cnpjCpf CNPJ do emitente
     * @param mod Modelo do Documento Fiscal
     * @param serie Série do Documento Fiscal
     * @param nNF Número do Documento Fiscal
     * @param tpEmis Forma de emissão da NF-e
     * @param cNF Código Numérico que compõe a Chave de Acesso
     *
     * @return the key
     */
    protected static String parseKey(String cUF, String dataAAMM, String cnpjCpf, String mod, String serie, String nNF,
            String tpEmis, String cNF, Boolean fullKey) throws ParseException {

        StringBuilder chave = new StringBuilder();
        chave.append(StringUtils.leftPad(cUF, 2, "0"));
        chave.append(StringUtils.leftPad(dataAAMM, 4, "0"));
        chave.append(StringUtils.leftPad(cnpjCpf.replaceAll("\\D", ""), 14, "0"));
        chave.append(StringUtils.leftPad(mod, 2, "0"));
        chave.append(StringUtils.leftPad(serie, 3, "0"));
        chave.append(StringUtils.leftPad(String.valueOf(nNF), 9, "0"));
        chave.append(StringUtils.leftPad(tpEmis, 1, "0"));
        chave.append(StringUtils.leftPad(cNF, 8, "0"));

        if (fullKey) {
            int digito = calculateDV(chave.toString());
            chave.append(digito);
        }

        return chave.toString();
    }

    /**
     * Parse Key
     *
     * @param chaveAcesso
     * @return
     * @throws ParseException
     */
    public static String parseKey(ChaveAcessoNFe chaveAcesso, Boolean fullKey) throws ParseException {

        return parseKey(chaveAcesso.cUF, chaveAcesso.dataAAMM, chaveAcesso.cnpjCpf, chaveAcesso.mod, chaveAcesso.serie,
                chaveAcesso.nNF, chaveAcesso.tpEmis, chaveAcesso.cNF, fullKey);

    }

    /**
     * Unparse Key
     *
     * @param chaveAcesso
     * @throws ParseException
     */
    public static ChaveAcessoNFe unparseKey(String chaveAcesso) throws ParseException {
        if (chaveAcesso == null) {
            throw new ParseException("Chave de Acesso não pode ser nula");
        }
        if (chaveAcesso.length() < 43 || chaveAcesso.length() > 44) {
            throw new ParseException("Chave de Acesso deve ter 43 ou 44 posições");
        }

        ChaveAcessoNFe chave = new ChaveAcessoNFe();
        chave.cUF = chaveAcesso.substring(0, 2);
        chave.dataAAMM = chaveAcesso.substring(2, 6);
        chave.cnpjCpf = chaveAcesso.substring(6, 20);
        chave.mod = chaveAcesso.substring(20, 22);
        chave.serie = chaveAcesso.substring(22, 25);
        chave.nNF = chaveAcesso.substring(25, 34);
        chave.tpEmis = chaveAcesso.substring(34, 35);
        chave.cNF = chaveAcesso.substring(35, 43);

        if (chaveAcesso.length() == 43) {
            int digito = calculateDV(chaveAcesso);
            chave.cDV = String.valueOf(digito);
        } else if (chaveAcesso.length() == 44) {
            chave.cDV = chaveAcesso.substring(43, 44);
        }

        return chave;
    }

    /**
     * Modulo11
     *
     * @param chave
     * @return
     */
    private static int modulo11(String chave) {
        int total = 0;
        int peso = 2;

        for (int i = 0; i < chave.length(); i++) {
            total += (chave.charAt((chave.length() - 1) - i) - '0') * peso;
            peso++;
            if (peso == 10) {
                peso = 2;
            }
        }
        int resto = total % 11;
        return (resto == 0 || resto == 1) ? 0 : (11 - resto);
    }

    public static void main(String[] args) {

        ChaveAcessoNFe nfe;
        try {
            nfe = unparseKey("35200233014556138384550010003216451890571008");
            System.out.println(nfe.nNF);
        } catch (ParseException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        //NFe35190305886614004476550450000068181211029358
        try {
            String cUF = "35";
            String dataAAMM = "2003";
            String cnpjCpf = "776574001551";
            String mod = "55";
            String serie = "045";
            String nNF = "000006900";
            String tpEmis = "1";
            String cNF = "21102935";

            String key = parseKey(cUF, dataAAMM, cnpjCpf, mod, serie, nNF, tpEmis, cNF, false);
            LOGGER.info("NFe" + key + calculateDV(cUF, dataAAMM, cnpjCpf, mod, serie, nNF, tpEmis, cNF));

        } catch (Exception e) {
            LOGGER.error(e.toString());
        }
    }

}
