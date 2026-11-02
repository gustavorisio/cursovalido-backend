package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AceiteTermosRequisicao(@NotBlank @Size(max = 30) String versaoTermos) {
}
