package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_matriculas", uniqueConstraints = @UniqueConstraint(
        name = "uk_matricula_aluno_curso", columnNames = { "aluno_id", "curso_id" }))
public class Matricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "aluno_id", nullable = false)
    private Long idAluno;
    @Column(name = "curso_id", nullable = false)
    private Long idCurso;
    @Column(name = "matriculado_em", nullable = false)
    private LocalDateTime matriculadoEm;
}
