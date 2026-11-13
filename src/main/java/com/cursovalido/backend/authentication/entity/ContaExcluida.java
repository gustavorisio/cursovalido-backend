package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "tb_contas_excluidas")
@Getter
@Setter
@NoArgsConstructor
public class ContaExcluida {
    @Id
    private Long id;
    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;
    @Column(name = "bloqueada_ate", nullable = false)
    private Instant bloqueadaAte;
}
