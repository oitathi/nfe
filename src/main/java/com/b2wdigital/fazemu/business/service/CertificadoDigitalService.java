package com.b2wdigital.fazemu.business.service;

import com.b2wdigital.assinatura_digital.CertificadoDigital;
import com.b2wdigital.fazemu.domain.CertificadoDigitalRedis;

/**
 * Certificado Digital Service.
 * 
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
public interface CertificadoDigitalService {

	CertificadoDigital getByIdEmissorRaiz(Long idEmissorRaiz);
	
	CertificadoDigitalRedis getInstance(Long idEmissorRaiz);

}
