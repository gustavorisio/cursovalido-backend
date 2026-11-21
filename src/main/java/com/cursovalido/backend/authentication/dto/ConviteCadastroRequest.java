package com.cursovalido.backend.authentication.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConviteCadastroRequest(
        @NotBlank(message = "O link do convite é inválido. Peça um novo convite à administração.") @Size(max = 43, message = "O link do convite é inválido. Peça um novo convite à administração.") String token,
        @NotNull(message = "Preencha os dados do cadastro.") @Valid CadastroUsuarioRequest cadastro) {
}
