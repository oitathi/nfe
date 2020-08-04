package com.b2wdigital.fazemu.service.impl;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.assinatura_digital.CertificadoDigital;
import com.b2wdigital.fazemu.business.repository.EmissorRaizCertificadoDigitalRepository;
import com.b2wdigital.fazemu.business.repository.ParametrosInfraRepository;
import com.b2wdigital.fazemu.business.service.CertificadoDigitalService;
import com.b2wdigital.fazemu.business.service.RedisOperationsService;
import com.b2wdigital.fazemu.domain.CertificadoDigitalRedis;
import com.b2wdigital.fazemu.domain.EmissorRaizCertificadoDigital;
import com.b2wdigital.fazemu.exception.FazemuServiceException;

/**
 * Certificado Digital Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class CertificadoDigitalServiceImpl implements CertificadoDigitalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificadoDigitalServiceImpl.class);

    private static final String CERTIFICADO = "CERTIFICADO_DIGITAL_";

    @Autowired
    private RedisOperationsService redisOperationsService;

    @Autowired
    private EmissorRaizCertificadoDigitalRepository emissorRaizCertificadoDigitalRepository;

    @Autowired
    private ParametrosInfraRepository parametrosInfraRepository;

    @Override
    public CertificadoDigital getByIdEmissorRaiz(Long idEmissorRaiz) {
        LOGGER.debug("CertificadoDigitalServiceImpl: getByIdEmissorRaiz");

        try {
            CertificadoDigitalRedis certificadoDigitalRedis = getInstance(idEmissorRaiz);

            CertificadoDigital certificadoDigital = CertificadoDigital.getInstance(
                    new ByteArrayInputStream(certificadoDigitalRedis.getCertificadoDigitalByte()),
                    new String(Base64.getDecoder().decode(certificadoDigitalRedis.getSenha())).toCharArray());

            return certificadoDigital;

        } catch (Exception e) {
            LOGGER.error("Certificado Digital nao disponivel para emissor raiz {}", idEmissorRaiz, e);
            throw new FazemuServiceException(e.getMessage(), e);
        }
    }

    @Override
    public CertificadoDigitalRedis getInstance(Long idEmissorRaiz) {

        // Verifica se o certificado está no cache
        CertificadoDigitalRedis certificadoDigitalRedis = (CertificadoDigitalRedis) redisOperationsService.getKeyValue(CERTIFICADO + idEmissorRaiz);

        // Caso nao esteja, busca no oracle
        if (certificadoDigitalRedis == null) {
            EmissorRaizCertificadoDigital emissorRaizCertificadoDigital = emissorRaizCertificadoDigitalRepository.findByIdEmissorRaiz(idEmissorRaiz);
            if (emissorRaizCertificadoDigital == null) {
                throw new FazemuServiceException("Certificado Digital nao cadastrado para o emissor raiz " + idEmissorRaiz);
            }

            certificadoDigitalRedis = CertificadoDigitalRedis.build(emissorRaizCertificadoDigital.getCertificadoBytes(), emissorRaizCertificadoDigital.getSenha());

            // Adiciona ao cache
            Integer tempoExpiracao = parametrosInfraRepository.getAsInteger(null, ParametrosInfraRepository.PAIN_EXPIRACAO_CERTIFICADO_DIGITAL, 6);
            redisOperationsService.setKeyValue(CERTIFICADO + idEmissorRaiz, certificadoDigitalRedis, tempoExpiracao.longValue(), TimeUnit.HOURS);
        }

        return certificadoDigitalRedis;
    }

}
