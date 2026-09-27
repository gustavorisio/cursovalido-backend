package com.cursovalido.backend.forum.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ConflitoException extends ResponseStatusException {
    public static final String TOPICO_FECHADO = "Topico fechado para novos comentarios";

    public ConflitoException(String mensagem) {
        super(HttpStatus.CONFLICT, mensagem);
    }

    public ConflitoException() {
        this(TOPICO_FECHADO);
    }

    public static ConflitoException topicoFechado() {
        return new ConflitoException(TOPICO_FECHADO);
    }
}
