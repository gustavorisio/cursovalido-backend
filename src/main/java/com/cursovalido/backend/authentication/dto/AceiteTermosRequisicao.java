package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.NotBlank;

public record AceiteTermosRequisicao(@NotBlank String versaoTermos) {
}
