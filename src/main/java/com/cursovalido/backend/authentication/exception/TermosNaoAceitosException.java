package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class TermosNaoAceitosException extends ResponseStatusException {
    public static final String CODIGO = "TERMOS_NAO_ACEITOS";
    private final String token;

    public TermosNaoAceitosException(String token) {
        super(HttpStatus.FORBIDDEN, "E necessario ler e aceitar os termos novamente");
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
