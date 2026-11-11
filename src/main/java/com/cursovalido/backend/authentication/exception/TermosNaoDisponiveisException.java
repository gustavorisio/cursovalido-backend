package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class TermosNaoDisponiveisException extends ResponseStatusException {
    public TermosNaoDisponiveisException() {
        super(HttpStatus.BAD_REQUEST, "A versao dos termos informada nao esta ativa");
    }
}