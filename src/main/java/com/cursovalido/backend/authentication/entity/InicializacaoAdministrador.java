package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_inicializacao_administrador")
@Getter
@Setter
@NoArgsConstructor
public class InicializacaoAdministrador {
    @Id
    private Long id = 1L;
    @Column(nullable = false)
    private boolean concluida;
    @Column(name = "id_usuario", unique = true)
    private Long idUsuario;
    @Column(name = "concluida_em")
    private Instant concluidaEm;

    public void concluir(Long idUsuario) {
        this.idUsuario = idUsuario;
        concluida = true;
        concluidaEm = Instant.now();
    }
}
