package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ServicoEmailIndisponivelException extends ResponseStatusException {
    public static final String CODIGO = "SERVICO_EMAIL_INDISPONIVEL";

    public ServicoEmailIndisponivelException() {
        super(HttpStatus.BAD_GATEWAY, "Nao foi possivel enviar o e-mail de confirmacao");
    }
}