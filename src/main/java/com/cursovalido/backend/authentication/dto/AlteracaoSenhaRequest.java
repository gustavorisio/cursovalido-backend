package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.validation.senha.SenhaValida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlteracaoSenhaRequest(
                @NotBlank @Size(max = 43) String token,
                @NotBlank @Size(max = 72, message = SenhaValida.MENSAGEM) @SenhaValida String novaSenha,
                @NotBlank @Size(max = 72, message = SenhaValida.MENSAGEM) @SenhaValida String confirmacaoSenha) {
}
