package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor
public class OpcaoCaminhos {
    @Column(nullable = false, length = 500)
    private String texto;
    @Column(name = "proxima_pergunta", length = 50)
    private String proximaPergunta;
    @Column(length = 50)
    private String resultado;
}
