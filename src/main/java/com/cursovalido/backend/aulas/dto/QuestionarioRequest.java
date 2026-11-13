package com.cursovalido.backend.aulas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record QuestionarioRequest(@NotBlank @Size(max = 255) String titulo, boolean publicado,
        @NotNull @Size(min = 1, max = 30) List<@NotNull @Valid Pergunta> perguntas) {
    public record Pergunta(@NotBlank @Size(max = 1000) String enunciado,
            @NotNull @Size(min = 2, max = 6) List<@NotBlank @Size(max = 500) String> alternativas,
            @NotNull @Min(0) @Max(5) Integer alternativaCorreta,
            @NotBlank @Size(max = 2000) String explicacao, @NotNull @Positive Long idAulaRevisao) {
    }
}
