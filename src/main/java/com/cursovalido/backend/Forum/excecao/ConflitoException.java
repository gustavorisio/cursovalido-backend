package com.cursovalido.backend.Forum.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ConflitoException extends ResponseStatusException {
    public ConflitoException(String mensagem) {
        super(HttpStatus.CONFLICT, mensagem);
    }
}
