package com.cursovalido.backend.aulas.dto;

import jakarta.validation.constraints.*;

public record SecaoCaminhosRequest(@NotBlank @Size(max = 80) String titulo,
        @NotBlank @Size(max = 2500) String conteudo, @Min(1) int ordem, boolean publicado) {}
