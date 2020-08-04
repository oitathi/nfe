package com.b2wdigital.fazemu.business.service;

import com.b2winc.corpserv.message.exception.BadRequestException;
import com.b2winc.corpserv.message.exception.UnauthorizedException;

public interface AccountService {
	
	String authenticateByToken(String userId, String token, String authorization, String system) throws UnauthorizedException, BadRequestException;

}
