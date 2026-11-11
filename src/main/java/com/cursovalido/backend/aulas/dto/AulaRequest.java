package com.cursovalido.backend.aulas.dto;

import jakarta.validation.constraints.*;

public record AulaRequest(
                @NotBlank @Size(max = 255) String titulo,
                @NotBlank @Size(max = 5000) String descricao,
                @NotNull @Positive Integer ordem,
                @NotBlank @Size(max = 2048) String youtubeUrl) {
}
