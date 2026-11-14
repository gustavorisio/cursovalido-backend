package com.cursovalido.backend.aulas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TemaRequest(@NotBlank @Size(max = 100) String nome) {
}
