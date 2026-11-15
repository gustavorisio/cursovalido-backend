package com.cursovalido.backend.notificacoes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComunicadoRequest(
        @NotBlank @Size(max = 255, message = "O título deve ter no máximo 255 caracteres.") String titulo,
        @NotBlank @Size(max = 5000, message = "O conteúdo deve ter no máximo 5.000 caracteres.") String conteudo) {
}
