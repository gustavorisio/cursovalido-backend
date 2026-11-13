package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_areas_tecnologia")
@Getter
@Setter
@NoArgsConstructor
public class AreaTecnologia {
    @Id
    private Long id;
    @Column(nullable = false, length = 50, unique = true)
    private String codigo;
    @Column(nullable = false, length = 100)
    private String nome;
    @Column(nullable = false, length = 1000)
    private String descricao;
    @Column(nullable = false, length = 1500)
    private String passos;
    @Column(name = "area_pai_id")
    private Long idAreaPai;
    @Column(nullable = false)
    private int ordem;
}
