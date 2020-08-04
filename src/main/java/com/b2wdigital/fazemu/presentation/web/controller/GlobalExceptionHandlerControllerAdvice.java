package com.b2wdigital.fazemu.presentation.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.b2winc.corpserv.message.exception.BadRequestException;
import com.b2winc.corpserv.message.exception.MethodNotAllowedException;
import com.b2winc.corpserv.message.exception.NotFoundException;
import com.b2winc.corpserv.message.exception.UnsuportedMediaTypeException;
import com.b2winc.message.error.AbstractError;
import com.b2winc.message.error.BadRequestError;
import com.b2winc.message.error.ErrorResponse;
import com.b2winc.message.error.MethodNotAllowedError;
import com.b2winc.message.error.NotFoundError;
import com.b2winc.message.error.UnsuportedMediaTypeError;
import com.b2winc.message.error.field.ValidationErrorEntity;

/**
 * Classe responsável em tratar Exceptions
 *
 */
@ControllerAdvice
@RestController
@CrossOrigin(origins = {"http://localhost:3000"})
public class GlobalExceptionHandlerControllerAdvice {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandlerControllerAdvice.class);

    /**
     * HttpStatus 404
     *
     * @param exc
     * @param response
     * @return
     * @throws com.b2winc.corpserv.message.exception.NotFoundException
     */
    @ExceptionHandler({NoHandlerFoundException.class})
    public ErrorResponse notFound(NoHandlerFoundException exc, HttpServletResponse response) throws NotFoundException {
        String message = exc.getRequestURL();
        NotFoundError error = new NotFoundError(response, null, "URL requisitada nao encontrada:[" + message + "]");
        return treatException(exc, error);
    }

    /**
     * HttpStatus 405
     *
     * @param exc
     * @param response
     * @return
     * @throws com.b2winc.corpserv.message.exception.MethodNotAllowedException
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public ErrorResponse notSupportedOperation(HttpRequestMethodNotSupportedException exc, HttpServletResponse response) throws MethodNotAllowedException {
        response.addHeader("Allow", exc.getSupportedHttpMethods().toString());
        return treatException(exc, new MethodNotAllowedError(response, null, "Metodo HTTP:[" + exc.getMethod() + "] nao suportado para a URL requisitada. Metodos suportados:" + exc.getSupportedHttpMethods() + ""));
    }

    /**
     * HttpStatus 415
     *
     * @return
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ErrorResponse mediaTypeNotAcceptable( HttpMediaTypeNotSupportedException exc ,HttpServletResponse response) throws UnsuportedMediaTypeException {
        UnsuportedMediaTypeError error = new UnsuportedMediaTypeError(response, null, "Content-Type não suportado!");
        return treatException(exc, error);
    }

    /**
     * HttpStatus 400
     *
     * @param exc
     * @param response
     * @return
     * @throws BadRequestException
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentNotValidException.class,
            MissingServletRequestPartException.class,
            HttpMessageNotReadableException.class,
            TypeMismatchException.class,
            ServletRequestBindingException.class})
    public ErrorResponse badRequest(Exception exc, HttpServletResponse response) throws BadRequestException {
   
    	ValidationErrorEntity validate = new ValidationErrorEntity();
    	validate.setMessage(exc.getMessage());
    	
    	List<ValidationErrorEntity> validateList =new ArrayList<ValidationErrorEntity>();
    	validateList.add(validate);
    	
    	BadRequestError bad = new BadRequestError(response);
    	bad.setValidationErrors(validateList);
    	
    	
        return treatException(exc, bad);
    }
    
    /**
     * HttpStatus 500
     *
     * @param exc
     * @param response
     * @return
     * @throws BadRequestException
     *//*
    @ExceptionHandler({
            InternalServerException.class})
    public ErrorResponse internalServerError(InternalServerException exc, HttpServletResponse response) throws InternalServerException {
    	InternalServerError error =  new InternalServerError(response);
    	error.setMessage(exc.getMessage());
    	error.setInfo(null);
    	
    	LOGGER.error("Erro ocorrido:["+error+"] pela Excecao:["+exc+"]");
        return error;
    }*/

    /**
     * Trata exceções
     * @param exc	Exceção
     * @param error	Erro a ser retornado
     * @return
     */
    public ErrorResponse treatException(Exception exc, AbstractError error){
        error.setErrorCode(null);
        error.setInfo(null);;
        LOGGER.warn("Erro ocorrido:["+error+"] pela Excecao:["+exc+"]");
        return error;
    }

}