/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.config;

import com.b2wdigital.fazemu.business.repository.UsuarioRepository;
import com.b2wdigital.fazemu.domain.Usuario;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

/**
 *
 * @author dailton.almeida
 */

@Service("authenticationProvider")
public class CustomAuthenticationProvider implements AuthenticationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomAuthenticationProvider.class);
    private static final List<SimpleGrantedAuthority> DEFAULT_AUTHORITY_LIST = Collections.singletonList(new SimpleGrantedAuthority("USER"));

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String principal = StringUtils.upperCase((String) authentication.getPrincipal());
        String credentials = (String) authentication.getCredentials();
        
        LOGGER.info("Usuario {} com senha a verificar ...", principal);
        Usuario usuario = usuarioRepository.findById(principal);
        
        if (usuario == null) {
            throw new BadCredentialsException("Usuario invalido no Fazemu " + principal);
        }

        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(principal, credentials, DEFAULT_AUTHORITY_LIST);
        result.setDetails(usuario.getNome());
        return result;
    }

    @Override
    public boolean supports(Class<?> authenticationClass) {
//        LOGGER.info("authenticationClass {}", authenticationClass);
        return UsernamePasswordAuthenticationToken.class.equals(authenticationClass);
    }

}
