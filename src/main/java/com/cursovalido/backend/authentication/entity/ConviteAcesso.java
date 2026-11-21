package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_convites_acesso")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConviteAcesso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "criado_por")
    @Setter
    private Long criadoPor;

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
    @Setter
    private boolean emailConfirmado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Setter
    private StatusConvite status;

    @Column(name = "enviado_em", nullable = false)
    private LocalDateTime enviadoEm;

    @Column(name = "quantidade_envios", nullable = false)
    private int quantidadeEnvios;

    public ConviteAcesso(String email, Perfil perfil, String tokenHash, LocalDateTime expiraEm) {
        this.email = email;
        this.perfil = perfil;
        this.tokenHash = tokenHash;
        this.criadoEm = LocalDateTime.now();
        this.expiraEm = expiraEm;
        this.status = StatusConvite.PENDENTE;
        this.enviadoEm = this.criadoEm;
        this.quantidadeEnvios = 1;
    }

    // Cada reenvio gera um novo link; o anterior deixa de funcionar.
    public void renovar(String novoTokenHash, LocalDateTime novaExpiracao) {
        tokenHash = novoTokenHash;
        expiraEm = novaExpiracao;
        emailConfirmado = false;
        status = StatusConvite.PENDENTE;
        enviadoEm = LocalDateTime.now();
        quantidadeEnvios++;
    }

    public boolean expirado() {
        return expiraEm.isBefore(LocalDateTime.now());
    }

    // O link recebido por e-mail comprova o acesso ao endereco; o convite so pode ser usado uma vez.
    public void concluir() {
        emailConfirmado = true;
        status = StatusConvite.CONCLUIDO;
    }
}
