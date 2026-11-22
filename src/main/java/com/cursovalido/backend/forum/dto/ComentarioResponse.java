package com.cursovalido.backend.forum.dto;

import java.time.LocalDateTime;

public record ComentarioResponse(
                Long id,
                String conteudo,
                String nomeAutor,
                String gamertagAutor,
                String papelAutor,
                Long idAutor,
                LocalDateTime criadoEm,
                boolean ativo) {
}