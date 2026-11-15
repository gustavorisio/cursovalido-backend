package com.cursovalido.backend.notificacoes.dto;

import java.time.LocalDateTime;

public record ComunicadoResponse(Long id, String titulo, String conteudo, LocalDateTime criadoEm, boolean lida) {
}
