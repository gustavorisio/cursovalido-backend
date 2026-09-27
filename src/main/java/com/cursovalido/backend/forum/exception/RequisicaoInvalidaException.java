package com.cursovalido.backend.forum.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RequisicaoInvalidaException extends ResponseStatusException {
    public static final String USUARIO_NAO_INFORMADO = "Usuario autenticado nao encontrado";
    public static final String COMENTARIO_APAGADO = "Comentario apagado nao pode ser editado";

    public RequisicaoInvalidaException(String mensagem) {
        super(HttpStatus.BAD_REQUEST, mensagem);
    }

    public RequisicaoInvalidaException() {
        this(USUARIO_NAO_INFORMADO);
    }

    public static RequisicaoInvalidaException usuarioNaoInformado() {
        return new RequisicaoInvalidaException(USUARIO_NAO_INFORMADO);
    }

    public static RequisicaoInvalidaException comentarioApagado() {
        return new RequisicaoInvalidaException(COMENTARIO_APAGADO);
    }
}
