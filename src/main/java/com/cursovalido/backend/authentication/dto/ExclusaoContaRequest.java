package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExclusaoContaRequest(
        @NotBlank(message = "Informe o código enviado ao seu e-mail.") @Pattern(regexp = "[0-9]{6}", message = "O código deve ter exatamente 6 números.") String codigo) {
    public ExclusaoContaRequest {
        if (codigo != null)
            codigo = codigo.trim();
    }
}
