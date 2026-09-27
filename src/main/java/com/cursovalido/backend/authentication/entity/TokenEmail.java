package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_tokens_email")
public class TokenEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 30)
    private String finalidade;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(nullable = false)
    private boolean utilizado;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected TokenEmail() {
    }

    public TokenEmail(String tokenHash, Usuario usuario, String finalidade, LocalDateTime expiraEm) {
        this.tokenHash = tokenHash;
        this.usuario = usuario;
        this.finalidade = finalidade;
        this.expiraEm = expiraEm;
        this.criadoEm = LocalDateTime.now();
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getFinalidade() {
        return finalidade;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }

    public boolean isUtilizado() {
        return utilizado;
    }

    public void setUtilizado(boolean valor) {
        utilizado = valor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}