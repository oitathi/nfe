package com.b2wdigital.fazemu.filter;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CorsFilterImpl implements CorsFilter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CorsFilterImpl.class);

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response) throws ServletException {		
		String allowedDomains = System.getenv("CORS_ALLOWED_DOMAINS");
		Boolean corsEnabled = Boolean.valueOf(System.getenv("CORS_ENABLED"));
		
		LOGGER.debug("#CORS - corsEnabled: "+corsEnabled);
		
		if (corsEnabled) {
			String clientOrigin = request.getHeader("origin");
			if(StringUtils.isNotBlank(clientOrigin)) {
				String originDomain = getDomainName(clientOrigin);
				
				Boolean isAllowed = true;
				if(StringUtils.isNotBlank(allowedDomains)) {
					isAllowed = StringUtils.contains("," + allowedDomains + ",", "," + originDomain + ",");
				}
				
				LOGGER.debug("#CORS - clientOrigin: " + clientOrigin);
				LOGGER.debug("#CORS - isAllowed: " + isAllowed);

				if(isAllowed) {
					response.setHeader("Access-Control-Allow-Origin", clientOrigin);
					response.setHeader("Access-Control-Allow-Credentials", "true");
					response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, OPTIONS, PATCH, DELETE");
					response.setHeader("Access-Control-Allow-Headers", "DNT, X-CustomHeader, Keep-Alive, User-Agent, X-Requested-With, If-Modified-Since, Cache-Control, Content-Type, Authorization, charset, Content-Encoding, Location, Allow, X-TID, WWW-Authenticate, X-Access-Control-Realm, internalId, Accept-Encoding, Accept-Language, Access-Control-Request-Headers, Access-Control-Request-Method, Connection, Host, Origin, Pragma, Referer, X-Preview, log, X-B2W-System, X-B2W-UserId, X-B2W-token, X-Authorization-UserPermissions, Access-Token");
					response.setHeader("Access-Control-Expose-Headers", "DNT, X-CustomHeader, Keep-Alive, User-Agent, X-Requested-With, If-Modified-Since, Cache-Control, Content-Type, Authorization, charset, Content-Encoding, Location, Allow, X-TID, WWW-Authenticate, X-Access-Control-Realm, internalId, Accept-Encoding, Accept-Language, Access-Control-Request-Headers, Access-Control-Request-Method, Connection, Host, Origin, Pragma, Referer, X-Preview, log, X-B2W-System, X-B2W-UserId, X-B2W-token, X-Authorization-UserPermissions, Access-Token");
				} 
			}
		}
		
	}

	
	private static String getDomainName(String url) throws ServletException {
		try {
			URI uri = new URI(url);
			String domain = uri.getHost();
			return domain.startsWith("www.") ? domain.substring(4) : domain;
		} catch (URISyntaxException e) {
			throw new ServletException("URL inválida: " + url, e);
		}
	}
}
