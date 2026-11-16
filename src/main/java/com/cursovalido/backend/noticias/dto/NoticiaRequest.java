package com.cursovalido.backend.noticias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticiaRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotBlank @Size(max = 30000) String conteudo) {
}
