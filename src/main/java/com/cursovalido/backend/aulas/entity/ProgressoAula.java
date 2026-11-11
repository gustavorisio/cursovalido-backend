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
@Table(name = "tb_progresso_aulas", uniqueConstraints = @UniqueConstraint(name = "uk_progresso_matricula_aula", columnNames = {
        "matricula_id", "aula_id" }))
public class ProgressoAula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "matricula_id", nullable = false)
    private Long idMatricula;
    @Column(name = "aula_id", nullable = false)
    private Long idAula;
    @Column(name = "posicao_segundos", nullable = false)
    private long posicaoSegundos;
    @Column(nullable = false)
    private boolean concluida;
    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
