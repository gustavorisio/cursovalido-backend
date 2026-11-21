package com.cursovalido.backend.aulas.dto;

public record RecompensaCursoResponse(Long idCurso, int xpGanho, int nivelAnterior, int nivelAtual,
        boolean subiuNivel, GamificacaoResponse situacao) {
}
