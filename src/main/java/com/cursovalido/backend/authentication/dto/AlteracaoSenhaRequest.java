package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.validation.senha.SenhaValida;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlteracaoSenhaRequest(
        @NotBlank(message = "O link de alteração de senha é inválido. Solicite um novo link.") @Size(max = 43, message = "O link de alteração de senha é inválido. Solicite um novo link.") String token,
        @NotBlank(message = "Informe a nova senha.") @SenhaValida String novaSenha,
        @NotBlank(message = "Confirme a nova senha.") @Size(max = 72, message = "A confirmação deve ter no máximo 72 caracteres.") String confirmacaoSenha) {

    @JsonIgnore
    @AssertTrue(message = "A confirmação deve ser igual à nova senha.")
    public boolean isConfirmacaoSenhaValida() {
        return novaSenha == null || confirmacaoSenha == null || novaSenha.equals(confirmacaoSenha);
    }

    @Override
    public String toString() {
        return "AlteracaoSenhaRequest[]";
    }
}
