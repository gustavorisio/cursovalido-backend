package com.cursovalido.backend.forum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_forum_users")
public class UsuarioForum {
    @Id
    @Column(nullable = false)
    private Long id;
    @Column(name = "name", nullable = false)
    private String nome;
    @Column(nullable = false)
    private String email;
    @Column(name = "role", nullable = false)
    private String perfil;
    @Column(name = "active", nullable = false)
    private boolean ativo = true;

    public UsuarioForum(Long id, String nome, String email, String perfil) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
    }

}