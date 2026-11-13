package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_caminhos_resultados")
@Getter @Setter @NoArgsConstructor
public class ResultadoCaminhos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionario_id", nullable = false)
    private QuestionarioCaminhos questionario;
    @Column(nullable = false, length = 50)
    private String chave;
    @Column(nullable = false, length = 255)
    private String titulo;
    @Column(nullable = false, length = 5000)
    private String orientacao;
    @Column(nullable = false)
    private int ordem;
    @ElementCollection
    @CollectionTable(name = "tb_caminhos_temas_resultado", joinColumns = @JoinColumn(name = "resultado_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "tema", nullable = false, length = 150)
    private List<String> temasPesquisa = new ArrayList<>();
}
