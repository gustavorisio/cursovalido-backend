package com.cursovalido.backend.authentication.dto;
import com.cursovalido.backend.authentication.validation.SenhaValida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AlteracaoSenhaAutenticadoRequest(
        @NotBlank @Size(max = 1024, message = "Informe uma senha valida.") String senhaAtual,
        @NotBlank @Size(min = 8, max = 72, message = "Informe uma senha valida.") @SenhaValida String novaSenha,
        @NotBlank @Size(min = 8, max = 72, message = "Informe uma senha valida.") @SenhaValida String confirmacaoSenha) {
}
