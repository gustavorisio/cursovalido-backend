package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_questoes")
@Getter
@Setter
@NoArgsConstructor
public class Questao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionario_id", nullable = false)
    private Questionario questionario;
    @Column(nullable = false, length = 1000)
    private String enunciado;
    @Column(nullable = false, length = 2000)
    private String explicacao;
    @Column(name = "alternativa_correta", nullable = false)
    private int alternativaCorreta;
    @Column(name = "aula_revisao_id", nullable = false)
    private Long idAulaRevisao;
    @Column(nullable = false)
    private int ordem;
    @ElementCollection
    @CollectionTable(name = "tb_alternativas_questao", joinColumns = @JoinColumn(name = "questao_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "texto", nullable = false, length = 500)
    private List<String> alternativas = new ArrayList<>();
}
