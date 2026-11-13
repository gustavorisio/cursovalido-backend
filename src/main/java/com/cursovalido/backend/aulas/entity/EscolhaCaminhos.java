package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor
public class EscolhaCaminhos {
    @Column(nullable = false, length = 50)
    private String pergunta;
    @Column(nullable = false)
    private int opcao;
}
