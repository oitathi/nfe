package com.b2wdigital.fazemu.business.service;

import com.b2wdigital.fazemu.domain.form.ConsultarRetornoForm;

/**
 *
 * @author dailton.almeida
 */
public interface ConsultarRetornoService {

    String consultarRetornoAsString(ConsultarRetornoForm form); //volta XML

    Object consultarRetornoAsObject(ConsultarRetornoForm form) throws Exception; //volta objeto JAXB
}
