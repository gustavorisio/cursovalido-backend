package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.validation.SenhaValida;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;
import java.time.LocalDate;
import java.util.Locale;

public record CadastroUsuarioRequest(
        @NotBlank @Size(max = 120) String nomeCompleto,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) @SenhaValida String senha,
        @NotBlank @Pattern(regexp = "\\d{11}") @CPF(message = "CPF invalido") String cpf,
        @NotNull @Past LocalDate dataNascimento,
        @NotNull @AssertTrue Boolean declarouMaiorIdade,
        @NotNull @AssertTrue Boolean aceitouTermosLgpd,
        @NotBlank @Size(max = 30) String versaoTermos) {
    public CadastroUsuarioRequest {
        if (email != null)
            email = email.trim().toLowerCase(Locale.ROOT);
    }
}
