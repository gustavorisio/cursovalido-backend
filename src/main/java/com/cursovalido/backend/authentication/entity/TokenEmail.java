package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_tokens_email")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    @Setter
    private boolean utilizado;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public TokenEmail(String tokenHash, Usuario usuario, String finalidade, LocalDateTime expiraEm) {
        this.tokenHash = tokenHash;
        this.usuario = usuario;
        this.finalidade = finalidade;
        this.expiraEm = expiraEm;
        this.criadoEm = LocalDateTime.now();
    }
}
