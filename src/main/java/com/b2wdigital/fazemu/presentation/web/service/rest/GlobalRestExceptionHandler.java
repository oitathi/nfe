package com.b2wdigital.fazemu.presentation.web.service.rest;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.b2wdigital.fazemu.exception.FazemuServiceException;
import com.b2wdigital.fazemu.exception.NotFoundException;
import com.b2winc.corpserv.message.exception.UnauthorizedException;
import com.google.common.collect.Maps;

/**
 *
 * @author dailton.almeida
 */
public class GlobalRestExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFoundException(NotFoundException e) {
        LOGGER.debug("handleNotFoundException {}", e.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleNotFoundException(UnauthorizedException e) {
        LOGGER.debug("UnauthorizedException {}", e.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(FazemuServiceException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleFazemuServiceException(FazemuServiceException e) {
        LOGGER.error("handleFazemuServiceException {}", e.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleException(Exception e) {
        LOGGER.error("handleException " + e.getMessage(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
    
    protected Map<String, String> buildResponse(HttpStatus httpStatus, String message) {
        Map<String, String> result = Maps.newHashMap();
        result.put("errorCode", String.valueOf(httpStatus.value()));
        result.put("message", message);
        return result;
    }
}
