package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MenorDeIdadeException extends ResponseStatusException {
    public MenorDeIdadeException() {
        super(HttpStatus.BAD_REQUEST, "Cadastro permitido somente para maiores de 18 anos");
    }
}