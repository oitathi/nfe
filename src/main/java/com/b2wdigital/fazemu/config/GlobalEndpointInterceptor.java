package com.b2wdigital.fazemu.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;

/**
 * Global Endpoint Interceptor.
 * 
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Component
public class GlobalEndpointInterceptor implements EndpointInterceptor {

    private static final Log LOGGER = LogFactory.getLog(GlobalEndpointInterceptor.class);

    @Override
    public boolean handleRequest(MessageContext messageContext, Object endpoint) throws Exception {
        LOGGER.debug("Global Request Handling");
        
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext, Object endpoint) throws Exception {
        LOGGER.debug("Global Response Handling");
        return true;
    }

    @Override
    public boolean handleFault(MessageContext messageContext, Object endpoint) throws Exception {
        LOGGER.debug("Global Exception Handling");
        return true;
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) throws Exception {
        LOGGER.debug("Execute Code After Completion");
    }
    
}
