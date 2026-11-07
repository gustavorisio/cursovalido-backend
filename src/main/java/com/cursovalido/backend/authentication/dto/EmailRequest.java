package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {
    public EmailRequest {
        if (email != null)
            email = email.trim().toLowerCase(Locale.ROOT);
    }
}
