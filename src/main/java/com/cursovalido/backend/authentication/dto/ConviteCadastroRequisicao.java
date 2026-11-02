package com.cursovalido.backend.authentication.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record ConviteCadastroRequisicao(@NotBlank String token,
                @Valid CadastroUsuarioRequisicao cadastro) {
}
