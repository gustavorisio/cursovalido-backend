package com.cursovalido.backend.aulas.dto;

import java.time.LocalDateTime;

public record ProgressoAulaResponse(Long idAula, long posicaoSegundos, boolean concluida,
        LocalDateTime concluidaEm, LocalDateTime atualizadoEm) {
}
