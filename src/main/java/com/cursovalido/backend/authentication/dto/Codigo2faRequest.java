package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record Codigo2faRequest(
        @NotBlank(message = "Informe seu e-mail.") @Email @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.") String email,
        @NotBlank(message = "Informe o código enviado ao seu e-mail.") @Pattern(regexp = "\\d{6}", message = "O código deve ter exatamente 6 números.") String codigo) {
    public Codigo2faRequest {
        if (email != null)
            email = email.trim().toLowerCase(Locale.ROOT);
        if (codigo != null)
            codigo = codigo.trim();
    }

    @Override
    public String toString() {
        return "Codigo2faRequest[email=" + email + "]";
    }
}
