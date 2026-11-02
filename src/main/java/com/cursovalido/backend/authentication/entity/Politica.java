package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_politicas")
public class Politica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome;
    @Column(nullable = false)
    private String versao;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;
    @Column(nullable = false)
    private boolean ativa;
    @Column(name = "data_publicacao", nullable = false)
    private LocalDateTime dataPublicacao;

    protected Politica() {
    }

    public Politica(String nome, String versao, String conteudo) {
        this.nome = nome;
        this.versao = versao;
        this.conteudo = conteudo;
        this.ativa = true;
        this.dataPublicacao = LocalDateTime.now();
    }

    public String getNome() {
        return nome;
    }

    public String getVersao() {
        return versao;
    }

    public String getConteudo() {
        return conteudo;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public LocalDateTime getDataPublicacao() {
        return dataPublicacao;
    }

    public void setAtiva(boolean valor) {
        ativa = valor;
    }
}