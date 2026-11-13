package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_caminhos_respostas_aluno", uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "questionario_id"}))
@Getter @Setter @NoArgsConstructor
public class RespostaAlunoCaminhos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "aluno_id", nullable = false)
    private Long idAluno;
    @Column(name = "questionario_id", nullable = false)
    private Long idQuestionario;
    @Column(nullable = false)
    private int versao;
    @Column(nullable = false, length = 50)
    private String resultado;
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
    @ElementCollection
    @CollectionTable(name = "tb_caminhos_escolhas_aluno", joinColumns = @JoinColumn(name = "resposta_id"))
    @OrderColumn(name = "ordem")
    private List<EscolhaCaminhos> respostas = new ArrayList<>();
}
