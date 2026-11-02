package com.cursovalido.backend.authentication.dto;

import java.time.LocalDateTime;

public record PoliticaResposta(String nome, String versao, String conteudo, LocalDateTime dataPublicacao) {
}