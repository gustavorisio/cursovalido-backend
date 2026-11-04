package com.cursovalido.backend.authentication.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ConviteAcessoInvalidoException extends RuntimeException {
    private final HttpStatus status;
    private final String codigo;

    public ConviteAcessoInvalidoException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }
}
