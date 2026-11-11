package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CadastroDuplicadoException extends ResponseStatusException {
    public CadastroDuplicadoException(String campo) {
        super(HttpStatus.CONFLICT, campo + " ja cadastrado");
    }
}