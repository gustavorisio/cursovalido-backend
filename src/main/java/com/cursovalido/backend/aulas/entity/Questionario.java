package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_questionarios")
@Getter
@Setter
@NoArgsConstructor
public class Questionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "curso_id", nullable = false)
    private Long idCurso;
    @Column(name = "modulo_id")
    private Long idModulo;
    @Column(nullable = false, length = 255)
    private String titulo;
    @Column(nullable = false)
    private boolean publicado;
    @Column(nullable = false)
    private int versao = 1;
    @OneToMany(mappedBy = "questionario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<Questao> perguntas = new ArrayList<>();
}
