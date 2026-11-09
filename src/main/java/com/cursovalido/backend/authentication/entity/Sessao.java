package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_sessoes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sessao {
    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;
    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;
    @Column(nullable = false)
    private boolean revogada;

    public Sessao(String id, Long usuarioId, LocalDateTime expiraEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.expiraEm = expiraEm;
    }

    public void revogar() {
        revogada = true;
    }
}
