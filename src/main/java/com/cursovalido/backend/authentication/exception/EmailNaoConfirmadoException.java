package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class EmailNaoConfirmadoException extends ResponseStatusException {
    public static final String CODIGO = "EMAIL_NAO_CONFIRMADO";

    public EmailNaoConfirmadoException() {
        super(HttpStatus.FORBIDDEN, "Confirme seu e-mail para entrar. Abra a mensagem de confirmação ou peça um novo envio.");
    }
}
