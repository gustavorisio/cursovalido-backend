package com.cursovalido.backend.Forum.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AcessoNegadoException extends ResponseStatusException {
    public AcessoNegadoException(String mensagem) {
        super(HttpStatus.FORBIDDEN, mensagem);
    }
}
