package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record ConviteAcessoRequisicao(@Email @Size(max = 254) @NotBlank String email,
                @NotNull Perfil perfil) {
    public ConviteAcessoRequisicao {
        if (email != null)
            email = email.trim().toLowerCase(Locale.ROOT);
    }
}
