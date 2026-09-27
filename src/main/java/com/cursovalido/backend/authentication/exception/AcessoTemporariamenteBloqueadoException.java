package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AcessoTemporariamenteBloqueadoException extends ResponseStatusException {
    public AcessoTemporariamenteBloqueadoException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Tente novamente mais tarde");
    }
}