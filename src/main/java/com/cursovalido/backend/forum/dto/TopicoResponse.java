package com.cursovalido.backend.forum.dto;

import java.time.LocalDateTime;

public record TopicoResponse(
        Long id,
        String titulo,
        String descricao,
        String nomeAutor,
        String papelAutor,
        Long idAutor,
        LocalDateTime criadoEm,
        long quantidadeRespostas,
        boolean ativo,
        boolean fechado) {
}