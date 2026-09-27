package com.cursovalido.backend.address.dto;

public record EnderecoResposta(String cep, String logradouro, String bairro, String localidade, String uf,
        Boolean erro) {
}