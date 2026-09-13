package com.cursovalido.backend.Forum.entidade;

import jakarta.persistence.*;

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

    public UsuarioForum() {
    }

    public UsuarioForum(Long id, String nome, String email, String perfil) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}