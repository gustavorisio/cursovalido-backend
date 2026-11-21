package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record LoginRequest(
        @NotBlank(message = "Informe seu e-mail.") @Email @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.") String email,
        @NotBlank(message = "Informe sua senha.") @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.") String senha) {
    public LoginRequest {
        if (email != null)
            email = email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
