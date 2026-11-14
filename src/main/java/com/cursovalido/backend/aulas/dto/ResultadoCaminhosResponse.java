package com.cursovalido.backend.aulas.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ResultadoCaminhosResponse(int versao, List<RespostasCaminhosRequest.Escolha> respostas,
        CaminhosRequest.Resultado resultado, LocalDateTime atualizadoEm) {}
