package com.cursovalido.backend.aulas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record RespostasCaminhosRequest(@NotNull @Positive Integer versao,
        @NotNull @Size(min = 1, max = 60) List<@NotNull @Valid Escolha> respostas) {
    public record Escolha(@NotBlank @Size(max = 50) String pergunta, @NotNull @Min(0) @Max(7) Integer opcao) {}
}
