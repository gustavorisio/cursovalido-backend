package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AcessoRejeitadoException extends ResponseStatusException {
    public static final String CODIGO = "ACESSO_REJEITADO";

    public AcessoRejeitadoException() {
        super(HttpStatus.FORBIDDEN, "Cadastro rejeitado pelo administrador");
    }
}
