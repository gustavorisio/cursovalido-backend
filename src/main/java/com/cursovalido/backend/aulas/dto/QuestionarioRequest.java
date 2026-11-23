package com.cursovalido.backend.aulas.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record QuestionarioRequest(@NotBlank @Size(max = 255) String titulo, boolean publicado,
                @NotNull @Size(min = 1, max = 30, message = "Cadastre de 1 a 30 perguntas.") List<@NotNull @Valid Pergunta> perguntas) {
        public record Pergunta(@NotBlank @Size(max = 1000) String enunciado,
                        @NotNull @Size(min = 2, max = 6, message = "Cada pergunta deve ter de 2 a 6 alternativas.") List<@NotBlank @Size(max = 500) String> alternativas,
                        @NotNull @Min(0) @Max(5) Integer alternativaCorreta,
                        @NotNull @Size(min = 2, max = 6) List<@NotNull @Size(max = 2000) String> explicacoes, @Positive Long idAulaRevisao) {
                @JsonIgnore
                @AssertTrue(message = "Informe uma explicação para cada alternativa incorreta.")
                public boolean isExplicacoesValidas() {
                        if (alternativas == null || explicacoes == null || alternativaCorreta == null
                                        || alternativaCorreta < 0 || alternativaCorreta >= alternativas.size()
                                        || explicacoes.size() != alternativas.size()) return false;
                        for (int i = 0; i < alternativas.size(); i++)
                                if (i != alternativaCorreta && (explicacoes.get(i) == null || explicacoes.get(i).isBlank())) return false;
                        return true;
                }
        }
}
