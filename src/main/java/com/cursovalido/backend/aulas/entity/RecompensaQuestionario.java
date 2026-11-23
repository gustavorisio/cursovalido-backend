package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_recompensas_questionarios", uniqueConstraints = @UniqueConstraint(
        name = "uk_recompensa_aluno_questionario", columnNames = { "aluno_id", "questionario_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecompensaQuestionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "aluno_id", nullable = false)
    private Long idAluno;
    @Column(name = "questionario_id", nullable = false)
    private Long idQuestionario;
    @Column(name = "xp_concedido", nullable = false)
    private int xpConcedido;
    @Column(name = "concedido_em", nullable = false)
    private LocalDateTime concedidoEm;

    public RecompensaQuestionario(Long idAluno, Long idQuestionario, int xpConcedido) {
        this.idAluno = idAluno;
        this.idQuestionario = idQuestionario;
        this.xpConcedido = xpConcedido;
        this.concedidoEm = LocalDateTime.now();
    }
}
