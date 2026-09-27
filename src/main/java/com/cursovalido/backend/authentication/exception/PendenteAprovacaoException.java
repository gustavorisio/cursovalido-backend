package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PendenteAprovacaoException extends ResponseStatusException {
    public static final String CODIGO = "PENDENTE_APROVACAO";

    public PendenteAprovacaoException() {
        super(HttpStatus.FORBIDDEN, "Cadastro aguardando aprovacao do administrador");
    }
}
