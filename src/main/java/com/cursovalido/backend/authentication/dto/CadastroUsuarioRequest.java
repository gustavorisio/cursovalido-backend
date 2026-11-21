package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.validation.senha.SenhaValida;
import jakarta.validation.constraints.*;
import java.text.Normalizer;
import java.util.Locale;

public record CadastroUsuarioRequest(
        @NotBlank(message = "Informe seu nome completo.") @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.") @Pattern(regexp = "(?s).*\\p{L}.*", message = "Informe um nome com letras.") String nomeCompleto,
        @NotBlank(message = "Informe seu e-mail.") @Email @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.") String email,
        @NotBlank(message = "Informe uma senha.") @SenhaValida String senha,
        @NotBlank(message = "Confirme a senha.") @Size(max = 72, message = "A confirmação deve ter no máximo 72 caracteres.") String confirmacaoSenha,
        @NotNull @AssertTrue(message = "Confirme que você tem 18 anos ou mais.") Boolean declarouMaiorIdade,
        @NotNull @AssertTrue(message = "Aceite os Termos de Uso e a Política de Privacidade para continuar.") Boolean aceitouTermosLgpd,
        @NotBlank(message = "Aceite os Termos de Uso e a Política de Privacidade para continuar.") @Size(max = 30) String versaoTermos,
        Boolean aceitouResponsabilidade,
        @Size(max = 30) String versaoResponsabilidade) {
    @Override
    public String toString() {
        return "CadastroUsuarioRequest[email=" + email + "]";
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    @AssertTrue(message = "As senhas devem coincidir.")
    public boolean isConfirmacaoSenhaValida() {
        // Campos vazios ja recebem a mensagem de preenchimento obrigatorio.
        return senha == null || confirmacaoSenha == null || senha.equals(confirmacaoSenha);
    }

    public CadastroUsuarioRequest {
        nomeCompleto = normalizarNome(nomeCompleto);
        if (email != null)
            email = email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizarNome(String nome) {
        if (nome == null)
            return null;
        return Normalizer.normalize(nome, Normalizer.Form.NFC)
                .replaceAll("[^\\p{L}\\p{M} ]", "").replaceAll(" +", " ").strip();
    }
}
