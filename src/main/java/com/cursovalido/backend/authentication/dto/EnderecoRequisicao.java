package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnderecoRequisicao(
                @NotBlank String cep,
                @NotBlank String logradouro,
                @NotBlank String bairro,
                @NotBlank String cidade,
                @NotBlank @Size(min = 2, max = 2) String estado) {
}