package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_tentativas_questionario")
@Getter
@Setter
@NoArgsConstructor
public class TentativaQuestionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "aluno_id", nullable = false)
    private Long idAluno;
    @Column(name = "questionario_id", nullable = false)
    private Long idQuestionario;
    @Column(nullable = false)
    private int versao;
    @Column(nullable = false)
    private int acertos;
    @Column(nullable = false)
    private int total;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
    @ElementCollection
    @CollectionTable(name = "tb_respostas_questionario", joinColumns = @JoinColumn(name = "tentativa_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "alternativa", nullable = false)
    private List<Integer> respostas = new ArrayList<>();
}
