package com.cursovalido.backend.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TopicoRequest(
        @NotBlank String titulo,
        @NotBlank @Size(max = 5000) String descricao) {
}