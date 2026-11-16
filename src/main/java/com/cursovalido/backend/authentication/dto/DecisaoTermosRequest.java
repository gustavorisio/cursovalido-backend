package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.NotNull;

public record DecisaoTermosRequest(@NotNull Boolean aceitou) {
}
