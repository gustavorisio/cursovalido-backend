package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import com.cursovalido.backend.authentication.security.CriptografiaDadosConverter;

@Embeddable
public class Endereco {
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(length = 512)
    private String cep;
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(length = 512)
    private String logradouro;
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(length = 512)
    private String bairro;
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(length = 512)
    private String cidade;
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(length = 512)
    private String estado;

    public Endereco() {
    }

    public Endereco(String cep, String logradouro, String bairro, String cidade, String estado) {
        this.cep = cep;
        this.logradouro = logradouro;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
    }

    public String getCep() {
        return cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getEstado() {
        return estado;
    }
}