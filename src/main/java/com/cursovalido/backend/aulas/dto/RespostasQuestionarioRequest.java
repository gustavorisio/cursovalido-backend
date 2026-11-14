package com.cursovalido.backend.aulas.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record RespostasQuestionarioRequest(@NotNull @Positive Integer versao,
        @NotNull @Size(min = 1, max = 30) List<@NotNull @Min(0) @Max(5) Integer> respostas) {
}
