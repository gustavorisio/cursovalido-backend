package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.validation.senha.SenhaValida;
import jakarta.validation.constraints.*;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;

public record CadastroUsuarioRequest(
        @NotBlank @Size(max = 120) @Pattern(regexp = "(?s).*\\p{L}.*", message = "Informe um nome com letras.") String nomeCompleto,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 72, message = SenhaValida.MENSAGEM) @SenhaValida String senha,
        @NotBlank @Size(max = 72) String confirmacaoSenha,
        @NotNull @Past LocalDate dataNascimento,
        @NotNull @AssertTrue(message = "Confirme que você tem 18 anos ou mais.") Boolean declarouMaiorIdade,
        @NotNull @AssertTrue(message = "Aceite os Termos de Uso e a Política de Privacidade para continuar.") Boolean aceitouTermosLgpd,
        @NotBlank @Size(max = 30) String versaoTermos,
        Boolean aceitouResponsabilidade,
        @Size(max = 30) String versaoResponsabilidade) {
    public CadastroUsuarioRequest(String nomeCompleto, String email, String senha, String confirmacaoSenha,
            LocalDate dataNascimento,
            Boolean declarouMaiorIdade, Boolean aceitouTermosLgpd, String versaoTermos) {
        this(nomeCompleto, email, senha, confirmacaoSenha, dataNascimento, declarouMaiorIdade, aceitouTermosLgpd,
                versaoTermos, null, null);
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    @AssertTrue(message = "As senhas devem coincidir.")
    public boolean isConfirmacaoSenhaValida() {
        return senha != null && senha.equals(confirmacaoSenha);
    }

    public CadastroUsuarioRequest {
        if (nomeCompleto != null)
            nomeCompleto = Normalizer.normalize(nomeCompleto, Normalizer.Form.NFC)
                    .replaceAll("[^\\p{L}\\p{M} ]", "").replaceAll(" +", " ").strip();
        if (email != null)
            email = email.trim().toLowerCase(Locale.ROOT);
    }
}
