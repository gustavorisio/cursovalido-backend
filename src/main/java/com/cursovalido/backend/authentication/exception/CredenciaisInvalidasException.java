package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CredenciaisInvalidasException extends ResponseStatusException {
    public static final String CODIGO = "CREDENCIAIS_INVALIDAS";

    public CredenciaisInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos");
    }
}
