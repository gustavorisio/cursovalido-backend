package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_conclusoes_cursos", uniqueConstraints = @UniqueConstraint(name = "uk_conclusao_aluno_curso", columnNames = {
        "aluno_id", "curso_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConclusaoCurso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aluno_id", nullable = false)
    private Long idAluno;

    @Column(name = "curso_id", nullable = false)
    private Long idCurso;

    @Column(name = "xp_concedido", nullable = false)
    private int xpConcedido;

    @Column(name = "concluido_em", nullable = false)
    private LocalDateTime concluidoEm;

    public ConclusaoCurso(Long idAluno, Long idCurso, int xpConcedido) {
        this.idAluno = idAluno;
        this.idCurso = idCurso;
        this.xpConcedido = xpConcedido;
        this.concluidoEm = LocalDateTime.now();
    }
}
