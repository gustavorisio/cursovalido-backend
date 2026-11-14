package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_caminhos_perguntas")
@Getter @Setter @NoArgsConstructor
public class PerguntaCaminhos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionario_id", nullable = false)
    private QuestionarioCaminhos questionario;
    @Column(nullable = false, length = 50)
    private String chave;
    @Column(nullable = false, length = 1000)
    private String enunciado;
    @Column(nullable = false)
    private int ordem;
    @ElementCollection
    @CollectionTable(name = "tb_caminhos_opcoes", joinColumns = @JoinColumn(name = "pergunta_id"))
    @OrderColumn(name = "ordem")
    private List<OpcaoCaminhos> opcoes = new ArrayList<>();
}
