package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_gamificacao_alunos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GamificacaoAluno {
    @Id
    @Column(name = "aluno_id")
    private Long idAluno;

    @Column(nullable = false)
    private int nivel = 1;

    @Column(name = "xp_nivel", nullable = false)
    private long xpNivel;

    @Column(name = "xp_total", nullable = false)
    private long xpTotal;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public void atualizar(int novoNivel, long novoXpNivel, long novoXpTotal) {
        this.nivel = novoNivel;
        this.xpNivel = novoXpNivel;
        this.xpTotal = novoXpTotal;
        this.atualizadoEm = LocalDateTime.now();
    }
}
