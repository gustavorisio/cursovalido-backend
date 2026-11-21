package com.cursovalido.backend.forum.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ConteudoOfensivoException extends ResponseStatusException {
    public static final String CODIGO = "CONTEUDO_OFENSIVO";
    public static final String MENSAGEM = "Seu texto contém palavras ofensivas, xingamentos ou termos pejorativos. "
            + "Revise o conteúdo antes de publicar.";

    public ConteudoOfensivoException() {
        super(HttpStatus.BAD_REQUEST, MENSAGEM);
    }
}
