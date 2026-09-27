package com.cursovalido.backend.forum.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RecursoNaoEncontradoException extends ResponseStatusException {
    public static final String COMENTARIO_NAO_ENCONTRADO = "Comentario nao encontrado";
    public static final String COMENTARIO_FORA_DO_TOPICO = "Comentario nao pertence ao topico";
    public static final String TOPICO_NAO_ENCONTRADO = "Topico nao encontrado";
    public static final String TOPICO_ARQUIVADO = "Topico arquivado";
    public static final String USUARIO_NAO_ENCONTRADO = "Usuario nao encontrado";

    public RecursoNaoEncontradoException(String mensagem) {
        super(HttpStatus.NOT_FOUND, mensagem);
    }

    public RecursoNaoEncontradoException() {
        this(TOPICO_NAO_ENCONTRADO);
    }

    public static RecursoNaoEncontradoException comentarioNaoEncontrado() {
        return new RecursoNaoEncontradoException(COMENTARIO_NAO_ENCONTRADO);
    }

    public static RecursoNaoEncontradoException comentarioForaDoTopico() {
        return new RecursoNaoEncontradoException(COMENTARIO_FORA_DO_TOPICO);
    }

    public static RecursoNaoEncontradoException topicoNaoEncontrado() {
        return new RecursoNaoEncontradoException(TOPICO_NAO_ENCONTRADO);
    }

    public static RecursoNaoEncontradoException topicoArquivado() {
        return new RecursoNaoEncontradoException(TOPICO_ARQUIVADO);
    }

    public static RecursoNaoEncontradoException usuarioNaoEncontrado() {
        return new RecursoNaoEncontradoException(USUARIO_NAO_ENCONTRADO);
    }
}
