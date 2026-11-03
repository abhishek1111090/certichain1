package com.certichain.server.web.controller;

import com.certichain.core.exception.CertiChainException;
import com.certichain.core.exception.CertificateNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Maps the custom exception hierarchy to friendly JSP error pages. */
@ControllerAdvice(basePackageClasses = GlobalExceptionHandler.class)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CertificateNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(CertificateNotFoundException e, Model model) {
        model.addAttribute("title", "Certificate not found");
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(CertiChainException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String domain(CertiChainException e, Model model) {
        log.warn("Request refused: {}", e.getMessage());
        model.addAttribute("title", "Operation refused");
        model.addAttribute("message", e.getMessage());
        return "error";
    }
}
