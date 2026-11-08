package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SuporteIndisponivelException extends ResponseStatusException {
    public static final String CODIGO = "SUPORTE_INDISPONIVEL";

    public SuporteIndisponivelException() {
        super(HttpStatus.SERVICE_UNAVAILABLE,
                "O suporte está temporariamente indisponível. Tente novamente mais tarde.");
    }
}
