package com.cursovalido.backend.authentication.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConviteCadastroRequest(@NotBlank @Size(max = 43) String token,
                @NotNull @Valid CadastroUsuarioRequest cadastro) {
}
