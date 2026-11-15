package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.validation.senha.SenhaValida;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Locale;

public record CadastroUsuarioRequest(
        @NotBlank @Size(max = 120) String nomeCompleto,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 72, message = SenhaValida.MENSAGEM) @SenhaValida String senha,
        @NotNull @Past LocalDate dataNascimento,
        @NotNull @AssertTrue Boolean declarouMaiorIdade,
        @NotNull @AssertTrue Boolean aceitouTermosLgpd,
        @NotBlank @Size(max = 30) String versaoTermos,
        Boolean aceitouResponsabilidade,
        @Size(max = 30) String versaoResponsabilidade) {
    public CadastroUsuarioRequest(String nomeCompleto, String email, String senha, LocalDate dataNascimento,
            Boolean declarouMaiorIdade, Boolean aceitouTermosLgpd, String versaoTermos) {
        this(nomeCompleto, email, senha, dataNascimento, declarouMaiorIdade, aceitouTermosLgpd,
                versaoTermos, null, null);
    }

    public CadastroUsuarioRequest {
        if (email != null)
            email = email.trim().toLowerCase(Locale.ROOT);
    }
}
