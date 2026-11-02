package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlteracaoSenhaRequisicao(
                @NotBlank String token,
                @NotBlank @Size(min = 8) String novaSenha,
                @NotBlank @Size(min = 8) String confirmacaoSenha) {
}