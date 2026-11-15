package com.cursovalido.backend.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class TermosNaoDisponiveisException extends ResponseStatusException {
    public TermosNaoDisponiveisException() {
        super(HttpStatus.BAD_REQUEST, "Os termos foram atualizados. Recarregue a página e aceite os documentos novamente.");
    }
}
