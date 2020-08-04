package com.b2wdigital.fazemu.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.repository.EmissorRaizRepository;
import com.b2wdigital.fazemu.business.repository.EstadoRepository;
import com.b2wdigital.fazemu.business.repository.UrlRepository;
import com.b2wdigital.fazemu.business.service.TipoEmissaoService;
import com.b2wdigital.fazemu.domain.EmissorRaiz;
import com.b2wdigital.fazemu.domain.Estado;
import com.b2wdigital.fazemu.exception.FazemuServiceException;

/**
 * AbstractNFe Service.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Service
public class AbstractNFeServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNFeServiceImpl.class);

    @Autowired
    protected EstadoRepository estadoRepository;

    @Autowired
    protected EmissorRaizRepository emissorRaizRepository;

    @Autowired
    protected UrlRepository urlRepository;

    @Autowired
    protected TipoEmissaoService tipoEmissaoService;

    /**
     * Get Url
     *
     * @param codigoIBGE
     * @param idTipoEmissao
     * @param idServico
     * @param versao
     * @return
     */
    protected String getUrl(Integer codigoIBGE, Integer idTipoEmissao, String idServico, String versao) {
        LOGGER.debug("AbstractNFeService: getUrl");

        String url = urlRepository.getUrl(codigoIBGE, idTipoEmissao, idServico, versao);

        if (url == null) {
            throw new FazemuServiceException("Impossivel encontrar URL para ufIBGE " + codigoIBGE
                    + " idTipoEmissao " + idTipoEmissao + " servico " + idServico + " versao " + versao);
        }

        return url;
    }

    protected Integer getTipoEmissao(String codigoIBGE) {
        return tipoEmissaoService.getTipoEmissaoByCodigoIBGE(codigoIBGE);
    }

    public Estado findEstadoByCodigoIbge(Integer codigoIbge) {
        return estadoRepository.findByCodigoIbge(codigoIbge);
    }

    public EmissorRaiz findEmissorRaizById(String id) {
        return emissorRaizRepository.findEmissorRaizById(id);
    }
}
