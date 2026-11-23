package com.cursovalido.backend.aulas.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ResultadoQuestionarioResponse(int acertos, int total, int percentual, LocalDateTime respondidoEm,
        List<Correcao> correcoes, List<Revisao> revisoes,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        RecompensaQuestionarioResponse recompensa) {
    public ResultadoQuestionarioResponse(int acertos, int total, int percentual, LocalDateTime respondidoEm,
            List<Correcao> correcoes, List<Revisao> revisoes) {
        this(acertos, total, percentual, respondidoEm, correcoes, revisoes, null);
    }
    public record Correcao(int pergunta, boolean acertou, Integer alternativaCorreta, String explicacao) {
    }

    public record Revisao(Long idAula, String titulo, Long idModulo) {
    }
}
