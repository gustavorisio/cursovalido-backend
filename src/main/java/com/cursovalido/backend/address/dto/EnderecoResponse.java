package com.cursovalido.backend.address.dto;

public record EnderecoResponse(String cep, String logradouro, String bairro, String localidade, String uf,
        Boolean erro) {
}