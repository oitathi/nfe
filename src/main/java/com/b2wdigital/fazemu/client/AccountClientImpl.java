package com.b2wdigital.fazemu.client;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.b2wdigital.fazemu.enumeration.RequestHeaderEnum;
import com.b2wdigital.fazemu.exception.FazemuClientException;
import com.b2winc.corpserv.message.exception.BadRequestException;
import com.b2winc.corpserv.message.exception.UnauthorizedException;

@Repository
public class AccountClientImpl implements AccountClient {

	@Value("${endpoint.account}")
    private String accountEnpoint;
	
	@Override
	public String authenticateByToken(String userId, String token, String authorization, String system) 
			throws UnauthorizedException, BadRequestException {

		String endpoint = accountEnpoint + "/" + userId + "/token/auth";
		
		RestTemplate restTemplate = new RestTemplate();
		
		try {

			HttpHeaders headers = getHeaders(userId, token, authorization, system);
			HttpEntity<Object> httpEntity = new HttpEntity<Object>(headers);

			ResponseEntity<String> response = restTemplate
					.exchange(URLDecoder.decode(endpoint, StandardCharsets.UTF_8.name()), HttpMethod.GET, httpEntity, String.class);
			
			return response.getBody();
			
		} catch(Exception e){
			if(e instanceof HttpStatusCodeException) {
				
				HttpStatusCodeException httpClientErrorException = (HttpStatusCodeException) e;
				
				if (StringUtils.equals(HttpStatus.UNAUTHORIZED.name(), httpClientErrorException.getStatusCode().name())) {
					UnauthorizedException unproc = new UnauthorizedException(e.getMessage());
					throw unproc;
				} else if (StringUtils.equals(HttpStatus.BAD_REQUEST.name(), httpClientErrorException.getStatusCode().name())) {
					BadRequestException unproc = new BadRequestException("Enpoint:" + endpoint +" Message:" + e.getMessage());
					throw unproc;
				}
			}
			
			throw new FazemuClientException(e);
		}
	}
	
	private HttpHeaders getHeaders(String userId, String token, String authorization, String system) {
		HttpHeaders headers = new HttpHeaders();
		
		headers.add(RequestHeaderEnum.USER.getValue(), userId);
		headers.add(RequestHeaderEnum.USER_TOKEN.getValue(), token);
		headers.add(RequestHeaderEnum.SYSTEM_NAME.getValue(), system);
		headers.add(HttpHeaders.AUTHORIZATION, "Basic " + authorization);
		headers.setContentType(MediaType.APPLICATION_JSON);

		return headers;
	}
}