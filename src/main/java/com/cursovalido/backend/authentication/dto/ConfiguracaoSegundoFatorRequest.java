package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.NotNull;

public record ConfiguracaoSegundoFatorRequest(@NotNull Boolean ativo) {
}
