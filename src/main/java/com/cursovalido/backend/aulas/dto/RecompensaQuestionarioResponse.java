package com.cursovalido.backend.aulas.dto;

public record RecompensaQuestionarioResponse(Long idQuestionario, int xpGanho, int nivelAnterior, int nivelAtual,
                boolean subiuNivel, GamificacaoResponse situacao) {
}
