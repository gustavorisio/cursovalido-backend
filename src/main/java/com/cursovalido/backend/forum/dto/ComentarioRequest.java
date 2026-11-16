package com.cursovalido.backend.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComentarioRequest(
                @NotBlank @Size(max = 5000) String conteudo) {
}