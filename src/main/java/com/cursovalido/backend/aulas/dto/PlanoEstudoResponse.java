package com.cursovalido.backend.aulas.dto;

import java.util.List;

public record PlanoEstudoResponse(List<Avaliacao> avaliacoes, List<ResultadoQuestionarioResponse.Revisao> revisoes) {
    public record Avaliacao(Long idModulo, String titulo, Integer percentual, int versao) {}
}
