package com.b2wdigital.fazemu.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.b2wdigital.fazemu.business.service.AccountService;
import com.b2wdigital.fazemu.enumeration.RequestHeaderEnum;

@Component
public class AuthorizationFilter implements Filter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationFilter.class);

	@Autowired
	private  CorsFilter corsFilter;
	
	@Autowired
	private AccountService accountService;
	
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest request = new HttpServletRequest((javax.servlet.http.HttpServletRequest) servletRequest);
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		
		corsFilter.doFilter(request, response);
		
		StringBuffer log = new StringBuffer("#AUTHORIZATIONFILTER ");
		
		log.append("requestURI:");
		log.append(request.getRequestURI());
		log.append(" getMethod:");
		log.append(request.getMethod());
		
		if(request.getRequestURI().contains("/fazemu-web/")  && 
				!request.getRequestURI().contains("/danfe") &&
				!request.getRequestURI().contains("/dacce") &&
				!request.getRequestURI().contains("/xml") && 
				!StringUtils.equals(request.getMethod(), "OPTIONS")) {
			
			String authorization	= request.getHeader(RequestHeaderEnum.AUTHORIZATION.getValue());
			String userId			= request.getHeader(RequestHeaderEnum.USER.getValue());	
			String token			= request.getHeader(RequestHeaderEnum.USER_TOKEN.getValue());
			String system			= request.getHeader(RequestHeaderEnum.SYSTEM_NAME.getValue());
			

			log.append(" param authorization:");
			log.append(authorization);
			log.append(" param userId:");
			log.append(userId);
			log.append(" param token:");
			log.append(token);
			log.append(" param system:");
			log.append(system);
			
			accountService.authenticateByToken(userId, token, authorization, system);
		}
		
		LOGGER.debug(log.toString());
		
		chain.doFilter(request, response);
		
	}
}