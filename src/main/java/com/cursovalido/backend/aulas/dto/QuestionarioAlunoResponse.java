package com.cursovalido.backend.aulas.dto;

import java.util.List;

public record QuestionarioAlunoResponse(Long id, String titulo, int versao, List<Pergunta> perguntas,
        ResultadoQuestionarioResponse resultado) {
    public record Pergunta(String enunciado, List<String> alternativas) {}
}
