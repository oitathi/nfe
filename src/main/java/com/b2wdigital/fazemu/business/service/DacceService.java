package com.b2wdigital.fazemu.business.service;

import java.io.OutputStream;
import com.itextpdf.text.Document;
import javax.xml.bind.JAXBException;

import com.b2wdigital.nfe.schema.v4v160b.cartacorrecao.envCCe_v1.TProcEvento;
import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;

/**
 * DACCE Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
public interface DacceService {

    Document fromChaveAcessoToPDF(String chaveAcesso, OutputStream outputStream) throws JAXBException;

    Document fromDacceToPDF(TNfeProc nfeProc, TProcEvento tProcEvento, OutputStream outputStream);
}
