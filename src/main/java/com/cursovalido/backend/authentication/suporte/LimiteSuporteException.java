package com.cursovalido.backend.authentication.suporte;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LimiteSuporteException extends ResponseStatusException {
    public static final String CODIGO = "LIMITE_SUPORTE_ATINGIDO";
    private final org.springframework.http.HttpHeaders cabecalhos = new org.springframework.http.HttpHeaders();

    public LimiteSuporteException(long segundos) {
        super(HttpStatus.TOO_MANY_REQUESTS,
                "Já existe uma solicitação recente para este IP ou e-mail. Aguarde 24 horas após o último envio para enviar novamente.");
        cabecalhos.set("Retry-After", Long.toString(segundos));
    }

    @Override
    public org.springframework.http.HttpHeaders getHeaders() {
        return cabecalhos;
    }
}
