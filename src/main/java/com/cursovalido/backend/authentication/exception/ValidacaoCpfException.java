package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ValidacaoCpfException extends ResponseStatusException {
    private final String codigo;

    public ValidacaoCpfException(HttpStatus status, String codigo, String mensagem) {
        super(status, mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() { return codigo; }
}
