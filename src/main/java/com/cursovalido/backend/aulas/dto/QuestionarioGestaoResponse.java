package com.cursovalido.backend.aulas.dto;

import java.util.List;

public record QuestionarioGestaoResponse(Long id, String titulo, boolean publicado, int versao,
        List<QuestionarioRequest.Pergunta> perguntas) {
}
