package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record EmailRequest(
        @NotBlank(message = "Informe seu e-mail.") @Email @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.") String email) {
    public EmailRequest {
        if (email != null)
            email = email.trim().toLowerCase(Locale.ROOT);
    }
}
