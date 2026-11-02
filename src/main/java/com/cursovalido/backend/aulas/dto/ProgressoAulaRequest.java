package com.cursovalido.backend.aulas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProgressoAulaRequest(@NotNull @PositiveOrZero Long posicaoSegundos) {
}
