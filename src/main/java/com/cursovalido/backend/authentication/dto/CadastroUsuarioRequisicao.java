package com.cursovalido.backend.authentication.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CadastroUsuarioRequisicao(
                @NotBlank String nomeCompleto,
                @NotBlank @Email String email,
                @NotBlank @Size(min = 8) String senha,
                @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
                @NotNull @Past LocalDate dataNascimento,
                @NotBlank String telefone,
                @NotNull @Valid EnderecoRequisicao endereco,
                @AssertTrue Boolean declarouMaiorIdade,
                @AssertTrue Boolean aceitouTermosLgpd,
                @NotBlank String versaoTermos) {
}