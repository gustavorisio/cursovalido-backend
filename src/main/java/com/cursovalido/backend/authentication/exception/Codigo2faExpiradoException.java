package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class Codigo2faExpiradoException extends ResponseStatusException {
    public static final String CODIGO = "CODIGO_2FA_EXPIRADO";

    public Codigo2faExpiradoException() {
        super(HttpStatus.BAD_REQUEST, "O codigo 2FA expirou");
    }
}