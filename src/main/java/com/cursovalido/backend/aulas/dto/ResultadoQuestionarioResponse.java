package com.cursovalido.backend.aulas.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ResultadoQuestionarioResponse(int acertos, int total, int percentual, LocalDateTime respondidoEm,
        List<Correcao> correcoes, List<Revisao> revisoes) {
    public record Correcao(int pergunta, boolean acertou, int alternativaCorreta, String explicacao) {}
    public record Revisao(Long idAula, String titulo, Long idModulo) {}
}
