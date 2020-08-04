package com.b2wdigital.fazemu.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.b2wdigital.fazemu.business.service.AccountService;
import com.b2wdigital.fazemu.client.AccountClient;
import com.b2winc.corpserv.message.exception.BadRequestException;
import com.b2winc.corpserv.message.exception.UnauthorizedException;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountClient accountClient;

    @Override
    public String authenticateByToken(String userId, String token, String authorization, String system) throws UnauthorizedException, BadRequestException {
        return accountClient.authenticateByToken(userId, token, authorization, system);
    }

}
