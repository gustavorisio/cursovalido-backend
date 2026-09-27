package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PoliticaRequisicao(
                @NotBlank @Size(max = 255) String nome,
                @NotBlank @Size(max = 30) String versao,
                @NotBlank @Size(max = 10000) String conteudo) {
}