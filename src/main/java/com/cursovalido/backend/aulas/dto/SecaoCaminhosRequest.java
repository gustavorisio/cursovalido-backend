package com.cursovalido.backend.aulas.dto;

import jakarta.validation.constraints.*;

public record SecaoCaminhosRequest(@NotBlank @Size(max = 255) String titulo,
        @NotBlank @Size(max = 20000) String conteudo, @Min(1) int ordem, boolean publicado) {}
