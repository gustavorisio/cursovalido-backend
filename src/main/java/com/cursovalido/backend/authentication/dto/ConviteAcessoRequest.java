package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record ConviteAcessoRequest(
        @NotBlank(message = "Informe o e-mail da pessoa convidada.") @Email @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.") String email,
        @NotNull(message = "Escolha o perfil do convite.") Perfil perfil) {
    public ConviteAcessoRequest {
        if (email != null)
            email = email.trim().toLowerCase(Locale.ROOT);
    }
}
