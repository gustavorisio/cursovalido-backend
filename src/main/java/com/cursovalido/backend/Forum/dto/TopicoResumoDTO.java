package com.cursovalido.backend.Forum.dto;

import java.time.LocalDateTime;

public record TopicoResumoDTO(
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