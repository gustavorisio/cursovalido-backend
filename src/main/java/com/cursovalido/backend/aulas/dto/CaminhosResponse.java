package com.cursovalido.backend.aulas.dto;

import java.util.List;

public record CaminhosResponse(Long id, String titulo, String descricao, boolean publicado, int versao,
        String perguntaInicial, List<CaminhosRequest.Pergunta> perguntas, List<CaminhosRequest.Resultado> resultados) {}
