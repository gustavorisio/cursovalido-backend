package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record ConviteAcessoRequisicao(@Email @jakarta.validation.constraints.NotBlank String email,
                @NotNull Perfil perfil) {
}
