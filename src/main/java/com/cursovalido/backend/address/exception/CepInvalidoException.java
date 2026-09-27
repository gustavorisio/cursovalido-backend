package com.cursovalido.backend.address.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CepInvalidoException extends ResponseStatusException {
    public CepInvalidoException() {
        super(HttpStatus.BAD_REQUEST, "CEP invalido ou nao encontrado");
    }
}