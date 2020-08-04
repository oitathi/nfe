package com.b2wdigital.fazemu.presentation.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author dailton.almeida
 */
@Controller
@CrossOrigin(origins = {"http://localhost:3000"})

public class LoginController {

    @GetMapping(value = "/home")
    public ModelAndView home() {
        return new ModelAndView("home");
    }

    @GetMapping(value = "/login")
    public ModelAndView login() {
        return new ModelAndView("login");
    }
    
}
