package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record Codigo2faRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "\\d{6}") String codigo) {
    public Codigo2faRequest {
        if (email != null)
            email = email.trim().toLowerCase(Locale.ROOT);
    }
}
