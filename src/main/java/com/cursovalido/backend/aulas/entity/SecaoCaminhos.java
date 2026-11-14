package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_caminhos_secoes")
@Getter @Setter @NoArgsConstructor
public class SecaoCaminhos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String titulo;
    @Column(nullable = false, length = 20000)
    private String conteudo;
    @Column(nullable = false)
    private int ordem = 1;
    @Column(nullable = false)
    private boolean publicado;
}
