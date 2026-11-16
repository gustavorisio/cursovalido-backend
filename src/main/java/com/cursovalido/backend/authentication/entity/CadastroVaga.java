package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_cadastro_vagas")
public class CadastroVaga {
    @Id
    @Column(length = 48)
    private String chave;

    protected CadastroVaga() {
    }
}
