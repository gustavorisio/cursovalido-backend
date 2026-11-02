package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenEmailRequisicao(@NotBlank @Size(max = 43) String token) {
}
