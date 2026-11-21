package com.cursovalido.backend.aulas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

public record ProgressoAulaResponse(Long idAula, long posicaoSegundos, boolean concluida,
        LocalDateTime concluidaEm, LocalDateTime atualizadoEm,
        @JsonInclude(JsonInclude.Include.NON_NULL) RecompensaCursoResponse recompensa) {
    public ProgressoAulaResponse(Long idAula, long posicaoSegundos, boolean concluida,
            LocalDateTime concluidaEm, LocalDateTime atualizadoEm) {
        this(idAula, posicaoSegundos, concluida, concluidaEm, atualizadoEm, null);
    }
}
