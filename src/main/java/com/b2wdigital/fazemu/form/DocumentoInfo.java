package com.b2wdigital.fazemu.form;

import org.w3c.dom.Document;

import lombok.Data;

import com.b2wdigital.nfe.schema.v4v160b.distdfe.resNFe_v1.ResNFe;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;

@Data
public class DocumentoInfo {

    private String conteudoDecode;
    private Document doc;
    private String ultimoNSU;
    private ResNFe resNFe;
    private TNfeProc tnfeProc;

    public DocumentoInfo(String conteudoDecode, Document doc, String ultimoNSU) {
        this.conteudoDecode = conteudoDecode;
        this.doc = doc;
        this.ultimoNSU = ultimoNSU;
    }

}
