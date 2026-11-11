package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_politicas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    @Setter
    private boolean ativa;
    @Column(name = "data_publicacao", nullable = false)
    private LocalDateTime dataPublicacao;

    public Politica(String nome, String versao, String conteudo) {
        this.nome = nome;
        this.versao = versao;
        this.conteudo = conteudo;
        this.ativa = true;
        this.dataPublicacao = LocalDateTime.now();
    }
}
