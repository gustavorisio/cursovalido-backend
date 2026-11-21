package com.cursovalido.backend.aulas.dto;

import jakarta.validation.constraints.Size;

public record VideoInicioRequest(@Size(max = 2048) String vimeoUrl) {
}
