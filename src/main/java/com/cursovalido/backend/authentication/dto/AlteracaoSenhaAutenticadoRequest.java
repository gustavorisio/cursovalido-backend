package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.validation.senha.SenhaValida;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlteracaoSenhaAutenticadoRequest(
        @NotBlank(message = "Informe sua senha atual.") @Size(max = 72, message = "A senha atual deve ter no máximo 72 caracteres.") String senhaAtual,
        @NotBlank(message = "Informe a nova senha.") @SenhaValida String novaSenha,
        @NotBlank(message = "Confirme a nova senha.") @Size(max = 72, message = "A confirmação deve ter no máximo 72 caracteres.") String confirmacaoSenha) {

    @JsonIgnore
    @AssertTrue(message = "A confirmação deve ser igual à nova senha.")
    public boolean isConfirmacaoSenhaValida() {
        return novaSenha == null || confirmacaoSenha == null || novaSenha.equals(confirmacaoSenha);
    }

    @Override
    public String toString() {
        return "AlteracaoSenhaAutenticadoRequest[]";
    }
}
