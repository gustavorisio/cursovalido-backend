package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_suspensoes_usuarios")
@Getter
@Setter
@NoArgsConstructor
public class SuspensaoUsuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;
    @Column(name = "id_administrador")
    private Long idAdministrador;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private EscopoSuspensao escopo;
    @Column(nullable = false, length = 1000)
    private String motivo;
    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm = Instant.now();
    @Column(name = "expira_em")
    private Instant expiraEm;
    @Column(name = "revogada_em")
    private Instant revogadaEm;

    public boolean vigente(Instant agora) {
        if (revogadaEm != null) return false;
        if (expiraEm == null) return true;
        return expiraEm.isAfter(agora);
    }
}
