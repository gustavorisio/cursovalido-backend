package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_caminhos_questionarios")
@Getter @Setter @NoArgsConstructor
public class QuestionarioCaminhos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String titulo;
    @Column(nullable = false, length = 2000)
    private String descricao;
    @Column(name = "pergunta_inicial", length = 50)
    private String perguntaInicial;
    @Column(nullable = false)
    private int versao = 1;
    @Column(nullable = false)
    private boolean publicado;
    @Column(nullable = false)
    private boolean ativo = true;
    @OneToMany(mappedBy = "questionario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<PerguntaCaminhos> perguntas = new ArrayList<>();
    @OneToMany(mappedBy = "questionario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<ResultadoCaminhos> resultados = new ArrayList<>();
}
