package com.b2wdigital.fazemu.presentation.web.service.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.b2winc.corpserv.message.exception.InternalServerException;
import com.b2winc.corpserv.message.exception.NotFoundException;

@Controller
public class HealthRestService {

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<String> hello(HttpServletRequest request, HttpServletResponse response)
            throws InternalServerException, IOException, NotFoundException {
        return new ResponseEntity<>("Hello Fazemu-NFe", HttpStatus.OK);
    }

    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public ResponseEntity<String> health(HttpServletRequest request, HttpServletResponse response)
            throws InternalServerException, IOException, NotFoundException {
        return new ResponseEntity<>("Health Fazemu-NFe", HttpStatus.OK);
    }

}
