package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class BanimentoException extends ResponseStatusException {
    public BanimentoException(String mensagem) {
        super(HttpStatus.FORBIDDEN, mensagem);
    }
}
