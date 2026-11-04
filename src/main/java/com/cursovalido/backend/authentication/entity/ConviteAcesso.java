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
    @Setter
    private boolean emailConfirmado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Setter
    private StatusConvite status;

    public ConviteAcesso(String email, Perfil perfil, String tokenHash, LocalDateTime expiraEm) {
        this.email = email;
        this.perfil = perfil;
        this.tokenHash = tokenHash;
        this.criadoEm = LocalDateTime.now();
        this.expiraEm = expiraEm;
        this.status = StatusConvite.PENDENTE;
    }

    public void renovar(String novoTokenHash, LocalDateTime novaExpiracao) {
        tokenHash = novoTokenHash;
        expiraEm = novaExpiracao;
        emailConfirmado = false;
        status = StatusConvite.PENDENTE;
    }

    public void prepararNovoConvite(Perfil novoPerfil, String novoTokenHash, LocalDateTime novaExpiracao) {
        perfil = novoPerfil;
        renovar(novoTokenHash, novaExpiracao);
    }
}
