package com.b2wdigital.fazemu.business.service;

import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.itextpdf.text.Document;
import java.io.OutputStream;
import javax.xml.bind.JAXBException;

/**
 *
 * @author dailton.almeida
 */
public interface DanfeService {
    Document fromChaveAcessoToPDF(String chaveAcesso, OutputStream outputStream) throws JAXBException;
    Document fromXmlNfeProcToPDF(String xmlNfeProc, OutputStream outputStream) throws JAXBException;
    Document fromNfeProcToPDF(TNfeProc nfeProc, OutputStream outputStream, Boolean isNFeExterna);
}
