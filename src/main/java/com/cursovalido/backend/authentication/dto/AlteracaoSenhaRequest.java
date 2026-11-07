package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.validation.SenhaValida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlteracaoSenhaRequest(
                @NotBlank @Size(max = 43) String token,
                @NotBlank @Size(min = 8, max = 72) @SenhaValida String novaSenha,
                @NotBlank @Size(min = 8, max = 72) @SenhaValida String confirmacaoSenha) {
}
