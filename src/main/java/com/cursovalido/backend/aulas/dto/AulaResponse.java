package com.cursovalido.backend.aulas.dto;

public record AulaResponse(Long id, String titulo, String descricao, Long idModulo,
        int ordem, String youtubeVideoId) {
}
