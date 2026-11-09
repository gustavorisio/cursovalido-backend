package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record ConviteAcessoRequest(@Email @Size(max = 254) @NotBlank String email,
        @NotNull Perfil perfil) {
    public ConviteAcessoRequest {
        if (email != null)
            email = email.trim().toLowerCase(Locale.ROOT);
    }
}
