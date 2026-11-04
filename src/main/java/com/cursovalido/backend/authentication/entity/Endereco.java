package com.cursovalido.backend.authentication.entity;

import com.cursovalido.backend.authentication.security.CriptografiaDadosConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Endereco {
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(length = 48)
    private String cep;
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(length = 440)
    private String logradouro;
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(length = 280)
    private String bairro;
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(length = 360)
    private String cidade;
    @Convert(converter = CriptografiaDadosConverter.class)
    @Column(length = 44)
    private String estado;
}
