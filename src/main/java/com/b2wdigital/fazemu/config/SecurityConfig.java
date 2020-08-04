/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.event.LoggerListener;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.PersonContextMapper;

/**
 *
 * @author dailton.almeida
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${fazemu.ldap.domain}")
    private String ldapDomain;
    @Value("${fazemu.ldap.url}")
    private String ldapUrl;
    @Value("${fazemu.ldap.rootDn}")
    private String ldapRootDn;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        ActiveDirectoryLdapAuthenticationProvider provider = new ActiveDirectoryLdapAuthenticationProvider(ldapDomain, ldapUrl, ldapRootDn);
        provider.setConvertSubErrorCodesToExceptions(true);
        provider.setUseAuthenticationRequestCredentials(true);
        provider.setUserDetailsContextMapper(new PersonContextMapper());
        LOGGER.info("########## Configurando provider {}", provider);
        auth.authenticationProvider(provider);
    }

    @Bean
    public LoggerListener loggerListener() {
        LoggerListener result = new LoggerListener();
        result.setLogInteractiveAuthenticationSuccessEvents(false);
        return result;
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/webjars/**").permitAll()
                .antMatchers("/health").permitAll()
                .antMatchers("/ws/**").permitAll()
                .antMatchers("/rest/**").permitAll()
                .antMatchers("/resource-status").permitAll()
                .antMatchers("/fazemu-web/**").permitAll()
                //.antMatchers("/login*").anonymous()
                .anyRequest().authenticated()
                
                .and()
                //.httpBasic()
                .formLogin()
                .loginPage("/login")
                .permitAll()
                .defaultSuccessUrl("/health", true)
                .failureUrl("/login?error=true")
                
                .and()
                .logout()
                .permitAll()
                .logoutUrl("/logout")
                .clearAuthentication(true)
                .invalidateHttpSession(true)
                .logoutSuccessUrl("/login")
                ;
    }
}