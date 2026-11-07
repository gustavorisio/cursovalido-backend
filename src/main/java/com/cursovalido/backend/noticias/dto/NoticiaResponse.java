package com.cursovalido.backend.noticias.dto;

import java.time.LocalDateTime;

public record NoticiaResponse(Long id, String titulo, String conteudo, Long idAutor,
        LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
}
