package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LimiteReenvioException extends ResponseStatusException {
    public LimiteReenvioException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "Limite de reenvio atingido. Tente novamente mais tarde");
    }
}