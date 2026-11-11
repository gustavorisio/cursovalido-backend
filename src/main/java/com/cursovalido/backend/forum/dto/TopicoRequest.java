package com.cursovalido.backend.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TopicoRequest(
        @NotBlank @Size(max = 14, message = "O titulo deve ter no maximo 14 caracteres") String titulo,
        @NotBlank @Size(max = 5000) String descricao) {
}
