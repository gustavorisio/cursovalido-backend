package com.cursovalido.backend.Forum.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RequisicaoInvalidaException extends ResponseStatusException {
    public RequisicaoInvalidaException(String mensagem) {
        super(HttpStatus.BAD_REQUEST, mensagem);
    }
}
