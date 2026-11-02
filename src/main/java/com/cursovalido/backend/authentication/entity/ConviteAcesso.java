package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_convites_acesso")
public class ConviteAcesso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "email_confirmado", nullable = false)
    private boolean emailConfirmado;

    @Column(nullable = false, length = 20)
    private String status;

    protected ConviteAcesso() {
    }

    public ConviteAcesso(String email, Perfil perfil, String tokenHash, LocalDateTime expiraEm) {
        this.email = email;
        this.perfil = perfil;
        this.tokenHash = tokenHash;
        this.criadoEm = LocalDateTime.now();
        this.expiraEm = expiraEm;
        this.status = "PENDENTE";
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }

    public boolean isEmailConfirmado() {
        return emailConfirmado;
    }

    public void setEmailConfirmado(boolean valor) {
        emailConfirmado = valor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String valor) {
        status = valor;
    }
}
