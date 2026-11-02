package com.cursovalido.backend.aulas.dto;

import jakarta.validation.constraints.*;

public record CursoRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotBlank @Size(max = 5000) String descricao) {
}
