package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenEmailRequisicao(@NotBlank String token) {
}