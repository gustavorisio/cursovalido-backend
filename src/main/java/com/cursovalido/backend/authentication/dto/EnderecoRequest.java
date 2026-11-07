package com.cursovalido.backend.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
                @NotBlank @Pattern(regexp = "\\d{8}") String cep,
                @NotBlank @Size(max = 100) String logradouro,
                @NotBlank @Size(max = 60) String bairro,
                @NotBlank @Size(max = 80) String cidade,
                @NotBlank @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO") String estado) {
}
